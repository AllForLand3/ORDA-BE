package com.allforland.orda.domain.analysis.dem.verification;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.dem.service.DemFileResolver;
import com.allforland.orda.domain.analysis.dem.service.DemRasterReader;
import com.allforland.orda.domain.analysis.dem.service.ElevationService;
import com.allforland.orda.domain.analysis.dem.vo.DemAssetRow;
import com.allforland.orda.domain.analysis.dem.vo.ElevationResult;
import com.allforland.orda.domain.analysis.dem.vo.ElevationStatus;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.tiff.GeoTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 기존 수동 Runner에서만 호출한다. DB 쓰기 및 자동 테스트 설정은 추가하지 않는다. */
final class DemManualVerification {
    private static final Logger LOG = LoggerFactory.getLogger(DemManualVerification.class);

    private DemManualVerification() {}

    static void run(DemRasterReader reader, DemAssetRow reference) throws Exception {
        Path folder = Files.createTempDirectory("orda-dem-step9-");
        try {
            Path raster = folder.resolve("samples.tif");
            writeRaster(raster, "-9999");
            DemFileResolver resolver = new DemFileResolver(folder.toString());
            DemAssetRow row = copy(reference, raster.toString(), "Y", "RAW", null);
            ElevationService service = service(row, resolver, reader);

            // PixelIsArea, 좌상단 (126, 38), 픽셀 크기 0.125도. 중심은 반 픽셀 안쪽이다.
            sample(service, 126.0625, 37.9375, ElevationStatus.VALID, 0.0);
            sample(service, 126.1875, 37.9375, ElevationStatus.VALID, -12.0);
            sample(service, 126.3125, 37.9375, ElevationStatus.NO_DATA, null);
            sample(service, 126.4375, 37.9375, ElevationStatus.INVALID_VALUE, null);
            sample(service, 126.0625, 37.8125, ElevationStatus.INVALID_VALUE, null);

            // 2진수로 정확히 표현되는 좌표로 경계와 동률을 확인한다.
            sample(service, 126.125 - 1e-8, 37.9375, ElevationStatus.VALID, 0.0);
            sample(service, 126.125, 37.9375, ElevationStatus.VALID, -12.0);
            sample(service, 126.125 + 1e-8, 37.9375, ElevationStatus.VALID, -12.0);
            sample(service, 126.1875, 37.875, ElevationStatus.VALID, 42.0);
            sample(service, 126.5 - 1e-8, 37.9375, ElevationStatus.INVALID_VALUE, null);
            sample(service, 126.0, 38.0, ElevationStatus.VALID, 0.0);
            sample(service, 126.0 - 1e-8, 37.9375, ElevationStatus.OUTSIDE_COVERAGE, null);
            sample(service, 126.0625, 38.0 + 1e-8, ElevationStatus.OUTSIDE_COVERAGE, null);
            sample(service, 126.5, 37.9375, ElevationStatus.OUTSIDE_COVERAGE, null);
            sample(service, 126.0625, 37.75, ElevationStatus.OUTSIDE_COVERAGE, null);
            sample(service, 126.1875, 37.75 + 1e-8, ElevationStatus.VALID, 42.0);

            Path nanRaster = folder.resolve("nan-nodata.tif");
            writeRaster(nanRaster, "nan");
            ElevationService nanService =
                    service(
                            copy(reference, nanRaster.toString(), "Y", "READY", null),
                            resolver,
                            reader);
            sample(nanService, 126.4375, 37.9375, ElevationStatus.NO_DATA, null);

            // DB NoData 대체 및 TIFF/DB 불일치도 DB 행 변경 없이 검증한다.
            Path noTag = folder.resolve("db-nodata.tif");
            writeRaster(noTag, null);
            sample(
                    service(
                            copy(reference, noTag.toString(), "Y", "RAW", new BigDecimal("-9999")),
                            resolver,
                            reader),
                    126.3125,
                    37.9375,
                    ElevationStatus.NO_DATA,
                    null);
            expect(
                    ErrorCode.DEM_METADATA_INVALID,
                    () ->
                            service(
                                            copy(
                                                    reference,
                                                    raster.toString(),
                                                    "Y",
                                                    "RAW",
                                                    new BigDecimal("-8888")),
                                            resolver,
                                            reader)
                                    .extract(1, 126.0625, 37.9375));

            expect(
                    ErrorCode.DEM_NOT_FOUND,
                    () -> service(null, resolver, reader).extract(1, 126, 38));
            for (String status : new String[] {"FAILED", "UNKNOWN", null}) {
                expect(
                        ErrorCode.DEM_UNAVAILABLE,
                        () ->
                                service(
                                                copy(
                                                        reference,
                                                        raster.toString(),
                                                        "Y",
                                                        status,
                                                        null),
                                                resolver,
                                                reader)
                                        .extract(1, 126, 38));
            }
            expect(
                    ErrorCode.DEM_UNAVAILABLE,
                    () ->
                            service(
                                            copy(reference, raster.toString(), "N", "READY", null),
                                            resolver,
                                            reader)
                                    .extract(1, 126, 38));
            expect(ErrorCode.DEM_INVALID_COORDINATE, () -> service.extract(1, 37.9375, 126.0625));

            expect(
                    ErrorCode.DEM_FILE_NOT_FOUND,
                    () ->
                            service(
                                            copy(
                                                    reference,
                                                    folder.resolve("missing.tif").toString(),
                                                    "Y",
                                                    "RAW",
                                                    null),
                                            resolver,
                                            reader)
                                    .extract(1, 126, 38));
            Path invalid = folder.resolve("invalid.tif");
            Files.writeString(invalid, "not a TIFF");
            expect(
                    ErrorCode.DEM_READ_FAILED,
                    () ->
                            service(
                                            copy(reference, invalid.toString(), "Y", "RAW", null),
                                            resolver,
                                            reader)
                                    .extract(1, 126, 38));

            if (!Files.getFileStore(raster).supportsFileAttributeView("posix")) {
                throw new IllegalStateException("Permission verification requires Linux/POSIX");
            }
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(raster);
            try {
                Files.setPosixFilePermissions(raster, Set.of());
                if (Files.isReadable(raster)) {
                    throw new IllegalStateException("Run verification as a non-root user");
                }
                expect(
                        ErrorCode.DEM_FILE_NOT_READABLE,
                        () -> service.extract(1, 126.0625, 37.9375));
            } finally {
                Files.setPosixFilePermissions(raster, permissions);
            }
            LOG.info(
                    "DEM_STEP9_VERIFICATION PASSED: zero=PASS, negative=PASS, noData=PASS, "
                            + "nanNoData=PASS, invalidPixels=PASS, boundaries=PASS, swappedAxes=PASS, "
                            + "missingDem=PASS, demStates=PASS, missingFile=PASS, permissions=PASS, format=PASS");
        } finally {
            cleanupTemporaryDirectory(folder);
        }
    }

    /** 한 항목의 정리 실패가 나머지 정리나 원래 검증 예외를 덮어쓰지 않도록 한다. */
    private static void cleanupTemporaryDirectory(Path folder) {
        try (var children = Files.list(folder)) {
            for (Path child : children.toList()) {
                deleteTemporaryPath(child);
            }
        } catch (IOException | UncheckedIOException | SecurityException exception) {
            LOG.warn("DEM 검증 임시 파일 목록을 확인하지 못했습니다.");
        } finally {
            deleteTemporaryPath(folder);
        }
    }

    /** 삭제 실패를 기록하고 다음 임시 자원의 정리를 계속한다. 내부 경로는 기록하지 않는다. */
    private static void deleteTemporaryPath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException exception) {
            LOG.warn("DEM 검증 임시 자원을 삭제하지 못했습니다.");
        }
    }

    private static ElevationService service(
            DemAssetRow row, DemFileResolver resolver, DemRasterReader reader) {
        // 상태 검증의 입력만 메모리에서 제공한다. 실제 Mapper 연결은 기존 Runner가 검증한다.
        return new ElevationService(id -> row, resolver, reader);
    }

    private static DemAssetRow copy(
            DemAssetRow source, String path, String use, String status, BigDecimal noData) {
        return new DemAssetRow(
                source.demId(),
                source.datasetId(),
                source.demName(),
                path,
                "EPSG:4326",
                source.resolutionM(),
                source.surfaceTypeCode(),
                noData,
                status,
                use);
    }

    private static void sample(
            ElevationService service,
            double longitude,
            double latitude,
            ElevationStatus expected,
            Double elevation) {
        ElevationResult result = service.extract(1, longitude, latitude);
        if (result.status() != expected
                || (elevation == null
                        ? result.elevationM() != null
                        : result.elevationM() == null
                                || Math.abs(result.elevationM() - elevation) > 1e-6)) {
            throw new IllegalStateException(
                    "Unexpected sample: expected="
                            + expected
                            + "/"
                            + elevation
                            + ", actual="
                            + result.status()
                            + "/"
                            + result.elevationM()
                            + ", longitude="
                            + longitude
                            + ", latitude="
                            + latitude);
        }
    }

    private static void expect(ErrorCode code, Runnable action) {
        try {
            action.run();
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == code) {
                return;
            }
            throw new IllegalStateException(
                    "Expected " + code + " but got " + exception.getErrorCode());
        }
        throw new IllegalStateException("Expected error: " + code);
    }

    private static void writeRaster(Path path, String noData) throws Exception {
        float[] values = {0, -12, -9999, Float.NaN, Float.POSITIVE_INFINITY, 42, 43, 44};
        var model =
                new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, 4, 2, 1, 4, new int[] {0});
        var raster =
                Raster.createWritableRaster(
                        model, new DataBufferFloat(values, values.length), null);
        var color =
                new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        false,
                        false,
                        Transparency.OPAQUE,
                        DataBuffer.TYPE_FLOAT);
        BufferedImage image = new BufferedImage(color, raster, false, null);
        ImageWriter writer = newTiffWriter();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            TIFFDirectory directory =
                    TIFFDirectory.createFromMetadata(
                            writer.getDefaultImageMetadata(
                                    ImageTypeSpecifier.createFromRenderedImage(image),
                                    writer.getDefaultWriteParam()));
            writeTags(directory, noData);
            writer.write(
                    null,
                    new IIOImage(image, null, directory.getAsMetadata()),
                    writer.getDefaultWriteParam());
        } finally {
            writer.dispose();
        }
    }

    static ImageWriter newTiffWriter() {
        var writers = ImageIO.getImageWritersByFormatName("TIFF");
        ImageWriter writer = null;
        while (writers.hasNext()) {
            ImageWriter candidate = writers.next();
            if (candidate.getClass().getName().startsWith("com.sun.imageio.plugins.tiff.")) {
                writer = candidate;
                break;
            }
            candidate.dispose();
        }
        if (writer == null) {
            throw new IllegalStateException(
                    "JDK TIFF writer is required for verification fixtures");
        }
        return writer;
    }

    private static void writeTags(TIFFDirectory directory, String noData) {
        directory.addTagSet(GeoTIFFTagSet.getInstance());
        directory.addTagSet(
                new TIFFTagSet(List.of(new TIFFTag("GDALNoData", 42113, 1 << TIFFTag.TIFF_ASCII))));
        char[] keys = {1, 1, 0, 3, 1024, 0, 1, 2, 1025, 0, 1, 1, 2048, 0, 1, 4326};
        directory.addTIFFField(
                new TIFFField(
                        new TIFFTag("GeoKeyDirectory", 34735, 1 << TIFFTag.TIFF_SHORT),
                        TIFFTag.TIFF_SHORT,
                        keys.length,
                        keys));
        directory.addTIFFField(
                new TIFFField(
                        new TIFFTag("ModelPixelScale", 33550, 1 << TIFFTag.TIFF_DOUBLE),
                        TIFFTag.TIFF_DOUBLE,
                        3,
                        new double[] {0.125, 0.125, 0}));
        directory.addTIFFField(
                new TIFFField(
                        new TIFFTag("ModelTiepoint", 33922, 1 << TIFFTag.TIFF_DOUBLE),
                        TIFFTag.TIFF_DOUBLE,
                        6,
                        new double[] {0, 0, 0, 126, 38, 0}));
        if (noData != null) {
            directory.addTIFFField(
                    new TIFFField(
                            new TIFFTag("GDALNoData", 42113, 1 << TIFFTag.TIFF_ASCII),
                            TIFFTag.TIFF_ASCII,
                            1,
                            new String[] {noData}));
        }
    }
}
