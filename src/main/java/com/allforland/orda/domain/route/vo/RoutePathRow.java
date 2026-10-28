package com.allforland.orda.domain.route.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 최단 경로 탐색 결과를 담는 데이터 객체다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RoutePathRow {
    private final Integer edgeCount;
    private final String edgeIds;
    private final BigDecimal lengthM;
    private final BigDecimal estTimeMin;
    private final String geometryJson;
}
