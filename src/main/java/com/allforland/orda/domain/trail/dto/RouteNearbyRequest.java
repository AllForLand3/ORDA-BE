package com.allforland.orda.domain.trail.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 경로 탐색 후보 주변 조회 요청 값을 담는다. edgeIds는 경로 탐색 응답 후보의 간선 ID 목록이다. */
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class RouteNearbyRequest {
    private String mountainId;

    private List<String> edgeIds;

    private Integer radiusM;
}
