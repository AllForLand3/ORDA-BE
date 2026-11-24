package com.allforland.orda.domain.analysis.terrain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 명시적으로 활성화한 기동에서만 등산로 구간의 DEM 고도 값을 백필한다. */
@Component
@ConditionalOnProperty(name = "orda.trail-edge-elevation-backfill.enabled", havingValue = "true")
public class TrailEdgeElevationBackfillRunner implements ApplicationRunner {

    private static final Logger LOG =
            LoggerFactory.getLogger(TrailEdgeElevationBackfillRunner.class);

    private final TrailEdgeElevationBackfillService service;
    private final long demId;

    public TrailEdgeElevationBackfillRunner(
            TrailEdgeElevationBackfillService service,
            @Value("${orda.trail-edge-elevation-backfill.dem-id}") long demId) {
        this.service = service;
        this.demId = demId;
    }

    @Override
    public void run(ApplicationArguments args) {
        int updatedCount = service.backfill(demId);
        LOG.info(
                "TRAIL_EDGE_ELEVATION_BACKFILL COMPLETED: demId={}, updatedEdges={}, intervalM=20.0",
                demId,
                updatedCount);
    }
}
