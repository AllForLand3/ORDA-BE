package com.allforland.orda.domain.analysis.dem.vo;

import lombok.Getter;
import lombok.experimental.Accessors;

/** 내부 고도 판독 결과다. 고도가 없는 경우 elevationM은 null이다. */
@Getter
@Accessors(fluent = true)
public final class ElevationResult {

    private final ElevationStatus status;
    private final Double elevationM;

    private ElevationResult(ElevationStatus status, Double elevationM) {
        this.status = status;
        this.elevationM = elevationM;
    }

    public static ElevationResult valid(double elevationM) {
        if (!Double.isFinite(elevationM)) {
            throw new IllegalArgumentException("고도는 유한한 값이어야 합니다.");
        }
        return new ElevationResult(ElevationStatus.VALID, elevationM);
    }

    public static ElevationResult outsideCoverage() {
        return new ElevationResult(ElevationStatus.OUTSIDE_COVERAGE, null);
    }

    public static ElevationResult noData() {
        return new ElevationResult(ElevationStatus.NO_DATA, null);
    }

    public static ElevationResult invalidValue() {
        return new ElevationResult(ElevationStatus.INVALID_VALUE, null);
    }
}
