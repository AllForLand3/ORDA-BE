package com.allforland.orda.domain.route.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 경로 탐색 응답 값을 담는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"mountainId", "startNodeId", "endNodeId", "candidates"})
public class RouteSearchResponse {
    private final String mountainId;
    private final String startNodeId;
    private final String endNodeId;
    private final List<RouteCandidateResponse> candidates;
}
