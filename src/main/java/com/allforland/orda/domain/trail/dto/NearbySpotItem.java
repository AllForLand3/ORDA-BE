package com.allforland.orda.domain.trail.dto;

import com.allforland.orda.common.geo.PointGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 주변 상세지점 응답 항목이다. */
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
    "sourceClass",
    "detail",
    "distanceM",
    "matchDistanceM",
    "verifyStatus",
    "tags",
    "geometry"
})
public class NearbySpotItem {
    private final String spotId;
    private final String sourceClass;
    private final String detail;
    private final BigDecimal distanceM;
    private final BigDecimal matchDistanceM;
    private final String verifyStatus;
    private final List<SpotTagItem> tags;
    private final PointGeometry geometry;
}
