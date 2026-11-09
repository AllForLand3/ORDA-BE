package com.allforland.orda.domain.analysis.terrain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 시작점부터의 거리와 EPSG:4326 샘플 좌표를 담는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class CourseSamplePoint {

    private final int seqNo;
    private final double distanceM;
    private final double longitude;
    private final double latitude;
}
