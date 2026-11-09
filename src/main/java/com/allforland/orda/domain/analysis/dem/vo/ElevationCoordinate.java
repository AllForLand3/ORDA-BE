package com.allforland.orda.domain.analysis.dem.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 고도 판독에 사용하는 EPSG:4326 경도·위도 좌표다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class ElevationCoordinate {
    private final double longitude;
    private final double latitude;
}
