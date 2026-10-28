package com.allforland.orda.domain.analysis.vo;

/** 좌표 한 점의 고도 판독 결과다. */
public enum ElevationStatus {
    VALID,
    OUTSIDE_COVERAGE,
    NO_DATA,
    INVALID_VALUE
}
