package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.domain.analysis.dem.service.ElevationService;
import com.allforland.orda.domain.analysis.dem.vo.ElevationCoordinate;
import com.allforland.orda.domain.analysis.dem.vo.ElevationResult;
import com.allforland.orda.domain.analysis.dem.vo.ElevationStatus;
import com.allforland.orda.domain.analysis.terrain.mapper.TrailEdgeElevationBackfillMapper;
import com.allforland.orda.domain.analysis.terrain.vo.TrailEdgeElevationSamplePoint;
import com.allforland.orda.domain.analysis.terrain.vo.TrailEdgeElevationUpdate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공용 DEM으로 등산로 구간의 정방향 누적 상승·하강을 한 번에 계산한다. */
@Service
@RequiredArgsConstructor
public class TrailEdgeElevationBackfillService {

    private static final BigDecimal SAMPLE_INTERVAL_M = new BigDecimal("20.0");
    private static final int UPDATE_BATCH_SIZE = 1_000;

    private final TrailEdgeElevationBackfillMapper mapper;
    private final ElevationService elevationService;

    /** MANUAL 값은 제외하고 UNKNOWN 또는 불완전한 DEM 값만 갱신한다. */
    @Transactional
    public int backfill(long demId) {
        if (demId <= 0) {
            throw new IllegalArgumentException("DEM ID는 양수여야 합니다.");
        }

        int candidateCount = mapper.countCandidates();
        if (candidateCount == 0) {
            return 0;
        }

        List<TrailEdgeElevationSamplePoint> points = mapper.findSamplePoints(SAMPLE_INTERVAL_M);
        Map<Long, List<TrailEdgeElevationSamplePoint>> pointsByEdge = groupByEdge(points);

        if (pointsByEdge.size() != candidateCount) {
            throw new IllegalStateException("유효하지 않은 형상의 등산로 구간이 있어 DEM 고도 백필을 중단합니다.");
        }

        List<ElevationCoordinate> coordinates =
                points.stream()
                        .map(point -> new ElevationCoordinate(point.longitude(), point.latitude()))
                        .toList();
        List<ElevationResult> elevations = elevationService.extractAll(demId, coordinates);

        if (elevations.size() != points.size()) {
            throw new IllegalStateException("DEM 고도 결과 건수가 샘플 좌표 건수와 다릅니다.");
        }

        List<TrailEdgeElevationUpdate> updates = new ArrayList<>(candidateCount);
        int cursor = 0;

        for (Map.Entry<Long, List<TrailEdgeElevationSamplePoint>> entry : pointsByEdge.entrySet()) {
            List<TrailEdgeElevationSamplePoint> edgePoints = entry.getValue();
            double ascentM = 0.0;
            double descentM = 0.0;
            Double firstElevationM = null;
            Double previousElevationM = null;
            Double lastElevationM = null;

            for (TrailEdgeElevationSamplePoint ignored : edgePoints) {
                ElevationResult elevation = elevations.get(cursor++);
                if (elevation == null
                        || elevation.status() != ElevationStatus.VALID
                        || elevation.elevationM() == null) {
                    throw new IllegalStateException(
                            "DEM 고도를 읽지 못한 등산로 구간이 있습니다: edgeId=" + entry.getKey());
                }

                double currentElevationM = elevation.elevationM();
                if (firstElevationM == null) {
                    firstElevationM = currentElevationM;
                }
                if (previousElevationM != null) {
                    double differenceM = currentElevationM - previousElevationM;
                    if (differenceM > 0.0) {
                        ascentM += differenceM;
                    } else if (differenceM < 0.0) {
                        descentM -= differenceM;
                    }
                }
                previousElevationM = currentElevationM;
                lastElevationM = currentElevationM;
            }

            if (firstElevationM == null || lastElevationM == null || edgePoints.size() < 2) {
                throw new IllegalStateException(
                        "DEM 고도 계산에 필요한 샘플이 부족합니다: edgeId=" + entry.getKey());
            }

            updates.add(
                    new TrailEdgeElevationUpdate(
                            entry.getKey(),
                            decimal(ascentM),
                            decimal(descentM),
                            lastElevationM > firstElevationM ? "Y" : "N"));
        }

        int updatedCount = 0;
        for (int start = 0; start < updates.size(); start += UPDATE_BATCH_SIZE) {
            int end = Math.min(start + UPDATE_BATCH_SIZE, updates.size());
            updatedCount += mapper.updateAll(updates.subList(start, end));
        }
        if (updatedCount != updates.size()) {
            throw new IllegalStateException(
                    "등산로 구간 고도 갱신 건수가 계산 결과와 다릅니다: expected="
                            + updates.size()
                            + ", actual="
                            + updatedCount);
        }

        return updatedCount;
    }

    private Map<Long, List<TrailEdgeElevationSamplePoint>> groupByEdge(
            List<TrailEdgeElevationSamplePoint> points) {
        Map<Long, List<TrailEdgeElevationSamplePoint>> grouped = new LinkedHashMap<>();
        for (TrailEdgeElevationSamplePoint point : points) {
            grouped.computeIfAbsent(point.edgeId(), ignored -> new ArrayList<>()).add(point);
        }
        return grouped;
    }

    private BigDecimal decimal(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalStateException("DEM 누적 고도 값이 올바르지 않습니다.");
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
