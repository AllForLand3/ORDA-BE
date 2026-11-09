package com.allforland.orda.domain.trail.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 주변 시설 조회 결과 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class NearbyFacilityRow {
    private final Long facilityId;
    private final String type;
    private final String name;
    private final String address;
    private final BigDecimal distanceM;
    private final String locationBasis;
    private final String verifyStatus;
    private final Double longitude;
    private final Double latitude;
}
