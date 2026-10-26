package com.allforland.orda.domain.analysis.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.vo.DemAssetRow;
import com.allforland.orda.domain.analysis.vo.ElevationResult;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemRasterReader {

    private static final Logger LOG = LoggerFactory.getLogger(DemRasterReader.class);

    /** DemFileResolver가 검증한 파일을 판독한다. longitude와 latitude는 EPSG:4326의 경도·위도 순서다. */
    public ElevationResult read(Path file, DemAssetRow dem, double longitude, double latitude) {

        GeoTiffReader reader = null;
        GridCoverage2D coverage = null;

        try {
            // GeoTools가 기본 CRS를 선택하거나 생성 단계에서 실패하기 전에 내부 키를 검사한다.
            CoordinateReferenceSystem declaredCrs = readInternalCrs(file);
            reader =
                    new GeoTiffReader(
                            file.toFile(),
                            new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

            GeoTiffIIOMetadataDecoder metadata = reader.getMetadata();

            coverage = reader.read(null);

            if (coverage == null) {
                throw new BusinessException(ErrorCode.DEM_READ_FAILED);
            }

            RenderedImage image = coverage.getRenderedImage();

            validateBand(coverage, image);

            CoordinateReferenceSystem rasterCrs = coverage.getCoordinateReferenceSystem2D();

            if (rasterCrs == null || !CRS.equalsIgnoreMetadata(declaredCrs, rasterCrs)) {
                throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
            }

            validateCrs(dem.crsCode(), rasterCrs);

            int dataType = image.getSampleModel().getDataType();
            Double noData = resolveNoData(metadata, dem, dataType);

            double[] rasterPoint = toRasterCoordinates(longitude, latitude, rasterCrs);

            double[] gridPoint = toGridCoordinates(coverage, rasterPoint);

            int minX = image.getMinX();
            int minY = image.getMinY();

            // getGridToCRS2D()의 격자 좌표는 픽셀 중심을 기준으로 한다.
            // 중심이 minX인 픽셀의 왼쪽 경계는 minX - 0.5다.
            double left = minX - 0.5;
            double top = minY - 0.5;
            double right = (double) minX + image.getWidth() - 0.5;
            double bottom = (double) minY + image.getHeight() - 0.5;

            if (gridPoint[0] < left
                    || gridPoint[0] >= right
                    || gridPoint[1] < top
                    || gridPoint[1] >= bottom) {
                return ElevationResult.outsideCoverage();
            }

            // 최근접 픽셀을 선택하며, 동률이면 더 큰 격자 좌표를 선택한다.
            int x = (int) Math.floor(gridPoint[0] + 0.5);
            int y = (int) Math.floor(gridPoint[1] + 0.5);

            if (x < minX
                    || y < minY
                    || x >= (long) minX + image.getWidth()
                    || y >= (long) minY + image.getHeight()) {
                return ElevationResult.outsideCoverage();
            }

            double value = image.getData(new Rectangle(x, y, 1, 1)).getSampleDouble(x, y, 0);

            if (noData != null && sameValue(value, noData)) {
                return ElevationResult.noData();
            }

            if (!Double.isFinite(value)) {
                return ElevationResult.invalidValue();
            }

            return ElevationResult.valid(value);

        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            // 내부 파일 경로나 외부 라이브러리 예외 메시지를 노출하지 않는다.
            throw new BusinessException(ErrorCode.DEM_READ_FAILED);
        } finally {
            release(coverage, reader);
        }
    }

    private CoordinateReferenceSystem readInternalCrs(Path file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                throw new BusinessException(ErrorCode.DEM_READ_FAILED);
            }
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.DEM_READ_FAILED);
            }
            ImageReader imageReader = readers.next();
            try {
                imageReader.setInput(input);
                return decodeInternalCrs(
                        new GeoTiffIIOMetadataDecoder(imageReader.getImageMetadata(0)));
            } finally {
                imageReader.dispose();
            }
        }
    }

    /** EPSG 코드가 명시된 2차원 지리·투영 CRS만 지원하며 기본 CRS 추정을 허용하지 않는다. */
    private CoordinateReferenceSystem decodeInternalCrs(GeoTiffIIOMetadataDecoder metadata) {
        if (metadata == null || !metadata.hasGeoKey()) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
        int model = requiredGeoKey(metadata, 1024);
        int key;
        if (model == 1) {
            key = 3072;
        } else if (model == 2) {
            key = 2048;
        } else {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
        int epsg = requiredGeoKey(metadata, key);
        if (epsg <= 0 || epsg >= 32767) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:" + epsg, true);
            if (crs.getCoordinateSystem().getDimension() != 2) {
                throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
            }
            return crs;
        } catch (FactoryException exception) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
    }

    private int requiredGeoKey(GeoTiffIIOMetadataDecoder metadata, int key) {
        String value = metadata.getGeoKey(key);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }
    }

    private void validateBand(GridCoverage2D coverage, RenderedImage image) {
        if (image == null
                || coverage.getNumSampleDimensions() != 1
                || image.getSampleModel().getNumBands() != 1
                || image.getWidth() <= 0
                || image.getHeight() <= 0) {
            throw new BusinessException(ErrorCode.DEM_METADATA_INVALID);
        }

        GridSampleDimension band = coverage.getSampleDimension(0);

        // 현재 단계에서는 별도 Scale/Offset 변환이 필요한 DEM을 허용하지 않는다.
        if (band.getScale() != 1.0 || band.getOffset() != 0.0) {
            throw new BusinessException(ErrorCode.DEM_METADATA_INVALID);
        }
    }

    private void validateCrs(String crsCode, CoordinateReferenceSystem rasterCrs) {

        if (crsCode == null || crsCode.isBlank() || rasterCrs == null) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }

        CoordinateReferenceSystem databaseCrs;

        try {
            databaseCrs = CRS.decode(crsCode.trim(), true);
        } catch (FactoryException exception) {
            throw new BusinessException(ErrorCode.DEM_CRS_INVALID);
        }

        if (!CRS.equalsIgnoreMetadata(databaseCrs, rasterCrs)) {
            throw new BusinessException(ErrorCode.DEM_CRS_MISMATCH);
        }
    }

    private double[] toRasterCoordinates(
            double longitude, double latitude, CoordinateReferenceSystem rasterCrs) {

        try {
            CoordinateReferenceSystem inputCrs = CRS.decode("EPSG:4326", true);

            MathTransform transform = CRS.findMathTransform(inputCrs, rasterCrs, false);

            double[] result = new double[2];

            transform.transform(new double[] {longitude, latitude}, 0, result, 0, 1);

            requireFinite(result);

            return result;
        } catch (FactoryException | TransformException exception) {
            throw new BusinessException(ErrorCode.DEM_TRANSFORM_FAILED);
        }
    }

    private double[] toGridCoordinates(GridCoverage2D coverage, double[] rasterPoint) {

        try {
            MathTransform inverse = coverage.getGridGeometry().getGridToCRS2D().inverse();

            double[] result = new double[2];

            inverse.transform(rasterPoint, 0, result, 0, 1);

            requireFinite(result);

            return result;
        } catch (TransformException exception) {
            throw new BusinessException(ErrorCode.DEM_TRANSFORM_FAILED);
        }
    }

    private void requireFinite(double[] point) {
        if (!Double.isFinite(point[0]) || !Double.isFinite(point[1])) {
            throw new BusinessException(ErrorCode.DEM_TRANSFORM_FAILED);
        }
    }

    private Double resolveNoData(
            GeoTiffIIOMetadataDecoder metadata, DemAssetRow dem, int dataType) {

        Double fileNoData =
                metadata.hasNoData() ? normalizeNoData(metadata.getNoData(), dataType) : null;

        Double databaseNoData =
                dem.noDataValue() == null
                        ? null
                        : normalizeNoData(dem.noDataValue().doubleValue(), dataType);

        if (fileNoData != null
                && databaseNoData != null
                && !sameValue(fileNoData, databaseNoData)) {
            throw new BusinessException(ErrorCode.DEM_METADATA_INVALID);
        }

        return fileNoData != null ? fileNoData : databaseNoData;
    }

    private double normalizeNoData(double value, int dataType) {
        if (dataType == DataBuffer.TYPE_FLOAT && Double.isFinite(value)) {
            float normalized = (float) value;

            if (!Float.isFinite(normalized)) {
                throw new BusinessException(ErrorCode.DEM_METADATA_INVALID);
            }

            return normalized;
        }

        return value;
    }

    private boolean sameValue(double first, double second) {
        return first == second || (Double.isNaN(first) && Double.isNaN(second));
    }

    private void release(GridCoverage2D coverage, GeoTiffReader reader) {
        try {
            if (coverage != null) {
                coverage.dispose(true);
            }
        } catch (RuntimeException exception) {
            LOG.warn("DEM coverage 자원 해제에 실패했습니다.");
        } finally {
            try {
                if (reader != null) {
                    reader.dispose();
                }
            } catch (RuntimeException exception) {
                LOG.warn("DEM reader 자원 해제에 실패했습니다.");
            }
        }
    }
}
