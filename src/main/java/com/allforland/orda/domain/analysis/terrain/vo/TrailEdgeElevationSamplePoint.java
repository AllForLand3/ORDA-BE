package com.allforland.orda.domain.analysis.terrain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 등산로 구간의 정방향 고도 계산에 사용하는 EPSG:4326 샘플 좌표다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class TrailEdgeElevationSamplePoint {

    private final long edgeId;
    private final int seqNo;
    private final double longitude;
    private final double latitude;
}
