package com.allforland.orda.domain.analysis.dem.verification;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.dem.mapper.DemAssetMapper;
import com.allforland.orda.domain.analysis.dem.service.DemFileResolver;
import com.allforland.orda.domain.analysis.dem.service.DemRasterReader;
import com.allforland.orda.domain.analysis.dem.service.ElevationService;
import com.allforland.orda.domain.analysis.dem.vo.DemAssetRow;
import com.allforland.orda.domain.analysis.dem.vo.ElevationResult;
import com.allforland.orda.domain.analysis.dem.vo.ElevationStatus;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.tiff.GeoTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.stream.ImageOutputStream;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 명시적으로 활성화한 경우에만 기준 TIFF와 고도 추출 서비스를 수동 검증한다. */
@Component
@Profile("dem-verification")
@ConditionalOnProperty(name = "orda.dem.verification.enabled", havingValue = "true")
public class DemTiffVerificationRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemTiffVerificationRunner.class);

    private static final int COLUMN = 3370;
    private static final int ROW = 691;

    // GDAL로 독립 판독한 기준 픽셀값이다.
    private static final double EXPECTED_ELEVATION = 137.000350952148;
    private static final double ELEVATION_TOLERANCE = 0.0001;
    private static final double COORDINATE_TOLERANCE = 1e-7;

    private final ElevationService elevationService;
    private final DemAssetMapper demAssetMapper;
    private final DemFileResolver demFileResolver;
    private final DemRasterReader demRasterReader;
    private final String filePath;
    private final long demId;

    public DemTiffVerificationRunner(
            ElevationService elevationService,
            DemAssetMapper demAssetMapper,
            DemFileResolver demFileResolver,
            DemRasterReader demRasterReader,
            @Value("${orda.dem.verification.file}") String filePath,
            @Value("${orda.dem.verification.dem-id}") long demId) {
        this.elevationService = elevationService;
        this.demAssetMapper = demAssetMapper;
        this.demFileResolver = demFileResolver;
        this.demRasterReader = demRasterReader;
        this.filePath = filePath;
        this.demId = demId;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        require(demId > 0, "Verification DEM ID must be positive");

        File file = new File(filePath);

        require(file.isFile() && file.canRead(), "Reference TIFF is not readable");

        GeoTiffReader reader =
                new GeoTiffReader(
                        file, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));

        GridCoverage2D coverage = null;

        try {
            coverage = reader.read(null);

            require(coverage != null, "Reference TIFF returned no coverage");

            RenderedImage image = coverage.getRenderedImage();

            require(image != null, "Reference TIFF returned no image");
            require(
                    image.getWidth() == 3600 && image.getHeight() == 3600,
                    "Unexpected reference raster dimensions");
            require(
                    coverage.getNumSampleDimensions() == 1
                            && image.getSampleModel().getNumBands() == 1,
                    "Expected one elevation band");

            CoordinateReferenceSystem geographic = CRS.decode("EPSG:4326", true);

            CoordinateReferenceSystem rasterCrs = coverage.getCoordinateReferenceSystem2D();

            require(
                    rasterCrs != null && CRS.equalsIgnoreMetadata(geographic, rasterCrs),
                    "Expected EPSG:4326 raster");

            int x = image.getMinX() + COLUMN;
            int y = image.getMinY() + ROW;

            double directElevation =
                    image.getData(new Rectangle(x, y, 1, 1)).getSampleDouble(x, y, 0);

            requireElevation(
                    directElevation, EXPECTED_ELEVATION, "Reference pixel differs from GDAL");

            // TIFF의 격자 변환을 이용해 기준 픽셀의 정확한 중심 좌표를 구한다.
            double[] rasterCenter = new double[2];

            coverage.getGridGeometry()
                    .getGridToCRS2D()
                    .transform(new double[] {x, y}, 0, rasterCenter, 0, 1);

            requireFinite(rasterCenter, "Pixel center is not finite");

            verifyProjectedRoundTrip(rasterCrs, rasterCenter);

            // 서비스 입력 계약인 EPSG:4326 [경도, 위도]로 변환한다.
            MathTransform toGeographic = CRS.findMathTransform(rasterCrs, geographic, false);

            double[] geographicCenter = new double[2];

            toGeographic.transform(rasterCenter, 0, geographicCenter, 0, 1);

            requireFinite(geographicCenter, "Geographic pixel center is not finite");

            ElevationResult result =
                    elevationService.extract(demId, geographicCenter[0], geographicCenter[1]);

            require(result != null, "Elevation service returned no result");
            require(
                    result.status() == ElevationStatus.VALID,
                    "Elevation service did not return VALID");
            require(result.elevationM() != null, "Elevation service returned no elevation");

            requireElevation(
                    result.elevationM(), EXPECTED_ELEVATION, "Service elevation differs from GDAL");

            requireElevation(
                    result.elevationM(),
                    directElevation,
                    "Service elevation differs from direct raster read");

            verifyStepSix(geographicCenter);
            require(
                    demAssetMapper.findById(Long.MAX_VALUE) == null,
                    "Missing DEM verification requires an unused ID");
            expectError(
                    ErrorCode.DEM_NOT_FOUND,
                    () ->
                            elevationService.extract(
                                    Long.MAX_VALUE, geographicCenter[0], geographicCenter[1]));
            DemManualVerification.run(demRasterReader, demAssetMapper.findById(demId));

            // 같은 Spring 서비스의 반복 호출에서도 Reader 자원을 공유하지 않는다.
            for (int attempt = 0; attempt < 5; attempt++) {
                ElevationResult repeated =
                        elevationService.extract(demId, geographicCenter[0], geographicCenter[1]);
                require(
                        repeated.status() == ElevationStatus.VALID && repeated.elevationM() != null,
                        "Repeated extraction failed");
                requireElevation(
                        repeated.elevationM(),
                        EXPECTED_ELEVATION,
                        "Repeated elevation differs from GDAL");
            }
            LOG.info("DEM_SERVICE_REUSE_VERIFICATION PASSED: calls=5");

            LOG.info(
                    "DEM_TIFF_VERIFICATION PASSED: width={}, height={}, bands=1, "
                            + "crs=EPSG:4326, column={}, row={}, elevation={}, "
                            + "epsg5179RoundTrip=PASS",
                    image.getWidth(),
                    image.getHeight(),
                    COLUMN,
                    ROW,
                    directElevation);

            LOG.info(
                    "DEM_ELEVATION_SERVICE_VERIFICATION PASSED: demId={}, "
                            + "longitude={}, latitude={}, status={}, elevationM={}",
                    demId,
                    geographicCenter[0],
                    geographicCenter[1],
                    result.status(),
                    result.elevationM());

        } finally {
            try {
                if (coverage != null) {
                    coverage.dispose(true);
                }
            } finally {
                reader.dispose();
            }
        }
    }

    private void verifyStepSix(double[] center) throws Exception {
        double[][] invalid = {
            {180.0001, 37},
            {-180.0001, 37},
            {127, 90.0001},
            {127, -90.0001},
            {Double.NaN, 37},
            {127, Double.NaN},
            {Double.POSITIVE_INFINITY, 37},
            {127, Double.NEGATIVE_INFINITY}
        };
        for (double[] coordinate : invalid) {
            expectError(
                    ErrorCode.DEM_INVALID_COORDINATE,
                    () -> elevationService.extract(demId, coordinate[0], coordinate[1]));
        }
        ElevationResult outside = elevationService.extract(demId, 0, 0);
        require(
                outside.status() == ElevationStatus.OUTSIDE_COVERAGE
                        && outside.elevationM() == null,
                "Expected outside coverage");

        DemAssetRow original = demAssetMapper.findById(demId);
        require(original != null, "Verification DEM was not found");
        Path registered = demFileResolver.resolve(original.filePath());
        DemAssetRow mismatch =
                new DemAssetRow(
                        original.demId(),
                        original.datasetId(),
                        original.demName(),
                        original.filePath(),
                        "EPSG:5179",
                        original.resolutionM(),
                        original.surfaceTypeCode(),
                        original.noDataValue(),
                        original.prepStatusCode(),
                        original.useYn());
        expectError(
                ErrorCode.DEM_CRS_MISMATCH,
                () -> demRasterReader.read(registered, mismatch, center[0], center[1]));

        // 원본·DB를 수정하지 않고 임시 TIFF로 CRS 누락과 불완전 정의를 확인한다.
        Path folder = Files.createTempDirectory("orda-dem-step6-");
        try {
            for (boolean incomplete : new boolean[] {false, true}) {
                Path fixture = folder.resolve(incomplete ? "incomplete.tif" : "missing.tif");
                writeCrsFixture(fixture, incomplete);
                try {
                    demRasterReader.read(fixture, original, center[0], center[1]);
                    throw new IllegalStateException("TIFF without complete CRS was accepted");
                } catch (BusinessException exception) {
                    require(
                            exception.getErrorCode() == ErrorCode.DEM_CRS_INVALID,
                            "Unexpected missing CRS error: " + exception.getErrorCode());
                    LOG.info(
                            "DEM_CRS_FIXTURE_REJECTED: incomplete={}, code={}",
                            incomplete,
                            exception.getErrorCode());
                } finally {
                    Files.deleteIfExists(fixture);
                }
            }
        } finally {
            Files.deleteIfExists(folder);
        }
        LOG.info(
                "DEM_STEP6_VERIFICATION PASSED: invalidCoordinates=PASS, outside=PASS, "
                        + "crsMismatch=PASS, missingCrs=PASS, incompleteCrs=PASS");
    }

    private static void expectError(ErrorCode expected, Runnable operation) {
        try {
            operation.run();
        } catch (BusinessException exception) {
            require(
                    exception.getErrorCode() == expected,
                    "Unexpected error: " + exception.getErrorCode());
            return;
        }
        throw new IllegalStateException("Expected error was not thrown: " + expected);
    }

    private static void writeCrsFixture(Path path, boolean incomplete) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_USHORT_GRAY);
        ImageWriter writer = DemManualVerification.newTiffWriter();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            TIFFDirectory directory =
                    TIFFDirectory.createFromMetadata(
                            writer.getDefaultImageMetadata(
                                    ImageTypeSpecifier.createFromRenderedImage(image),
                                    writer.getDefaultWriteParam()));
            if (incomplete) {
                directory.addTagSet(GeoTIFFTagSet.getInstance());
                // 지리 좌표계 모델이지만 EPSG CRS 키 2048이 누락된 정의다.
                char[] keys = {1, 1, 0, 2, 1024, 0, 1, 2, 1025, 0, 1, 1};
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
                                new double[] {0.001, 0.001, 0}));
                directory.addTIFFField(
                        new TIFFField(
                                new TIFFTag("ModelTiepoint", 33922, 1 << TIFFTag.TIFF_DOUBLE),
                                TIFFTag.TIFF_DOUBLE,
                                6,
                                new double[] {0, 0, 0, 126, 38, 0}));
            }
            writer.write(
                    null,
                    new IIOImage(image, null, directory.getAsMetadata()),
                    writer.getDefaultWriteParam());
        } finally {
            writer.dispose();
        }
    }

    private static void verifyProjectedRoundTrip(
            CoordinateReferenceSystem rasterCrs, double[] rasterCenter) throws Exception {

        CoordinateReferenceSystem projected = CRS.decode("EPSG:5179", true);

        MathTransform forward = CRS.findMathTransform(rasterCrs, projected, false);

        double[] projectedPoint = new double[2];
        double[] restored = new double[2];

        forward.transform(rasterCenter, 0, projectedPoint, 0, 1);

        requireFinite(projectedPoint, "Projected coordinates are not finite");

        forward.inverse().transform(projectedPoint, 0, restored, 0, 1);

        requireFinite(restored, "Restored coordinates are not finite");

        require(
                Math.abs(rasterCenter[0] - restored[0]) < COORDINATE_TOLERANCE
                        && Math.abs(rasterCenter[1] - restored[1]) < COORDINATE_TOLERANCE,
                "EPSG:4326/5179 round-trip mismatch");
    }

    private static void requireElevation(double actual, double expected, String message) {

        require(
                Double.isFinite(actual) && Math.abs(actual - expected) <= ELEVATION_TOLERANCE,
                message);
    }

    private static void requireFinite(double[] coordinates, String message) {

        require(Double.isFinite(coordinates[0]) && Double.isFinite(coordinates[1]), message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
