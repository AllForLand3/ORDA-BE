package com.allforland.orda.domain.route.dto;

import com.allforland.orda.common.geo.LineStringGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 경로 후보 하나의 응답 값을 담는다. */
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
    "rank",
    "priorityCondition",
    "lengthM",
    "estTimeMin",
    "stairCount",
    "edgeIds",
    "geometry"
})
public class RouteCandidateResponse {
    private final int rank;
    private final String priorityCondition;
    private final BigDecimal lengthM;
    private final BigDecimal estTimeMin;

    /** 경로가 지나는 구간의 계단 지점 수 합계(trail_edge.stair_spot_cnt). 주변 반경 조회 수와 다르다. */
    private final int stairCount;

    private final List<String> edgeIds;
    private final LineStringGeometry geometry;
}
