package com.allforland.orda.domain.trail.dto;

import com.allforland.orda.common.geo.PointGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 시설 상세 조회 응답(I-04)이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "facilityId",
    "mountainId",
    "type",
    "name",
    "address",
    "locationBasis",
    "verifyStatus",
    "duplicateOfFacilityId",
    "source",
    "geometry"
})
public class FacilityDetailResponse {
    private final String facilityId;
    private final String mountainId;
    private final String type;
    private final String name;
    private final String address;
    private final String locationBasis;
    private final String verifyStatus;
    private final String duplicateOfFacilityId;
    private final FacilitySourceItem source;
    private final PointGeometry geometry;
}
