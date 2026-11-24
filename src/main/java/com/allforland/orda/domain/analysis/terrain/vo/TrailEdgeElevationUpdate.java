package com.allforland.orda.domain.analysis.terrain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** DEM 샘플로 계산한 등산로 구간의 정방향 누적 상승·하강 값이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class TrailEdgeElevationUpdate {

    private final long edgeId;
    private final BigDecimal ascentM;
    private final BigDecimal descentM;
    private final String uphillYn;
}
