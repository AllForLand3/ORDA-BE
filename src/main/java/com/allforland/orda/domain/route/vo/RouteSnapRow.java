package com.allforland.orda.domain.route.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 좌표 스냅 결과를 담는 데이터 객체다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RouteSnapRow {
    private final Long nodeId;
    private final BigDecimal distanceM;
    private final Double longitude;
    private final Double latitude;
}
