package com.allforland.orda.domain.trail.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 경로 탐색 후보 주변 상세지점·시설 조회 응답이다. spots는 I-01, facilities는 I-02와 같은 기준이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"mountainId", "radiusM", "spots", "facilities"})
public class RouteNearbyResponse {
    private final String mountainId;
    private final int radiusM;
    private final RouteNearbyGroup<NearbySpotItem> spots;
    private final RouteNearbyGroup<NearbyFacilityItem> facilities;
}
