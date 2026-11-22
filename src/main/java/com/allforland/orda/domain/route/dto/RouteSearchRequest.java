package com.allforland.orda.domain.route.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 경로 탐색 요청 값을 담는다. */
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class RouteSearchRequest {
    private String mountainId;

    private String startNodeId;

    private String endNodeId;

    /** 탐색 기준 코드 목록. 없거나 비어 있으면 거리 우선만 탐색한다. */
    private List<String> priorityConditions;
}
