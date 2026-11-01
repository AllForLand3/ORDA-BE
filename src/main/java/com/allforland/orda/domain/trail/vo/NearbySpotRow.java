package com.allforland.orda.domain.trail.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 주변 상세지점 조회 결과 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class NearbySpotRow {
    private final Long spotId;
    private final String sourceClass;
    private final String detail;
    private final BigDecimal distanceM;
    private final BigDecimal matchDistanceM;
    private final String verifyStatus;
    private final Double longitude;
    private final Double latitude;
}
