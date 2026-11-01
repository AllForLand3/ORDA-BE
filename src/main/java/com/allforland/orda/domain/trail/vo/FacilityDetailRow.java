package com.allforland.orda.domain.trail.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 시설 단건 조회 결과 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class FacilityDetailRow {
    private final Long facilityId;
    private final Long mountainId;
    private final String type;
    private final String name;
    private final String address;
    private final String locationBasis;
    private final String verifyStatus;
    private final Long duplicateOfFacilityId;
    private final Long datasetId;
    private final String datasetName;
    private final String provider;
    private final String baseDate;
    private final String collectDate;
    private final Double longitude;
    private final Double latitude;
}
