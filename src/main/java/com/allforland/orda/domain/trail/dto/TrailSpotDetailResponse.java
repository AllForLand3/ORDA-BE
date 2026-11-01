package com.allforland.orda.domain.trail.dto;

import com.allforland.orda.common.geo.PointGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 상세지점 상세 조회 응답(I-03)이다. */
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
    "spotId",
    "mountainId",
    "datasetId",
    "sourceSpotId",
    "sourceClass",
    "detail",
    "etc",
    "verifyStatus",
    "duplicateOfSpotId",
    "tags",
    "matches",
    "geometry"
})
public class TrailSpotDetailResponse {
    private final String spotId;
    private final String mountainId;
    private final String datasetId;
    private final String sourceSpotId;
    private final String sourceClass;
    private final String detail;
    private final String etc;
    private final String verifyStatus;
    private final String duplicateOfSpotId;
    private final List<SpotTagItem> tags;
    private final List<SpotMatchItem> matches;
    private final PointGeometry geometry;
}
