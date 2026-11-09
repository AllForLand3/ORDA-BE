package com.allforland.orda.domain.trail.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 상세지점 단건 조회 결과 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SpotDetailRow {
    private final Long spotId;
    private final Long mountainId;
    private final Long datasetId;
    private final String sourceSpotId;
    private final String sourceClass;
    private final String detail;
    private final String etc;
    private final String verifyStatus;
    private final Long duplicateOfSpotId;
    private final Double longitude;
    private final Double latitude;
}
