package com.allforland.orda.domain.analysis.verification;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.File;
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

/** Explicitly enabled, read-only verification for the team's Copernicus reference file. */
@Component
@Profile("dem-verification")
@ConditionalOnProperty(name = "orda.dem.verification.enabled", havingValue = "true")
public class DemTiffVerificationRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemTiffVerificationRunner.class);
    private static final int COLUMN = 3370;
    private static final int ROW = 691;
    // Independently read from the full-resolution file with GDAL gdallocationinfo.
    private static final double EXPECTED = 137.000350952148;
    private static final double ELEVATION_TOLERANCE = 0.0001;
    private final String filePath;

    public DemTiffVerificationRunner(@Value("${orda.dem.verification.file}") String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
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
            require(
                    image.getWidth() == 3600 && image.getHeight() == 3600,
                    "Unexpected reference raster dimensions");
            require(coverage.getNumSampleDimensions() == 1, "Expected one elevation band");
            CoordinateReferenceSystem geographic = CRS.decode("EPSG:4326", true);
            CoordinateReferenceSystem rasterCrs = coverage.getCoordinateReferenceSystem2D();
            require(CRS.equalsIgnoreMetadata(geographic, rasterCrs), "Expected EPSG:4326 raster");

            int x = image.getMinX() + COLUMN;
            int y = image.getMinY() + ROW;
            double elevation = image.getData(new Rectangle(x, y, 1, 1)).getSampleDouble(x, y, 0);
            require(
                    Double.isFinite(elevation)
                            && Math.abs(elevation - EXPECTED) <= ELEVATION_TOLERANCE,
                    "Reference pixel differs from GDAL");

            // Grid-to-CRS uses the pixel center; do not use rounded QGIS click coordinates.
            double[] center = new double[2];
            coverage.getGridGeometry()
                    .getGridToCRS2D()
                    .transform(new double[] {x, y}, 0, center, 0, 1);
            CoordinateReferenceSystem projected = CRS.decode("EPSG:5179", true);
            MathTransform forward = CRS.findMathTransform(rasterCrs, projected, false);
            double[] projectedPoint = new double[2];
            double[] restored = new double[2];
            forward.transform(center, 0, projectedPoint, 0, 1);
            require(
                    Double.isFinite(projectedPoint[0]) && Double.isFinite(projectedPoint[1]),
                    "Projected coordinates are not finite");
            forward.inverse().transform(projectedPoint, 0, restored, 0, 1);
            require(
                    Math.abs(center[0] - restored[0]) < 1e-7
                            && Math.abs(center[1] - restored[1]) < 1e-7,
                    "EPSG:4326/5179 round-trip mismatch");
            LOG.info(
                    "DEM_TIFF_VERIFICATION PASSED: width={}, height={}, bands=1, "
                            + "crs=EPSG:4326, column={}, row={}, elevation={}, "
                            + "epsg5179RoundTrip=PASS",
                    image.getWidth(),
                    image.getHeight(),
                    COLUMN,
                    ROW,
                    elevation);
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

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
