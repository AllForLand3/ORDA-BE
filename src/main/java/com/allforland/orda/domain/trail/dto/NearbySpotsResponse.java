package com.allforland.orda.domain.trail.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 주변 상세지점 조회 응답(I-01)이다. counts는 태그별 상세지점 수다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"courseId", "revNo", "radiusM", "dataAvailable", "counts", "items"})
public class NearbySpotsResponse {
    private final String courseId;
    private final int revNo;
    private final int radiusM;
    private final boolean dataAvailable;
    private final Map<String, Integer> counts;
    private final List<NearbySpotItem> items;
}
