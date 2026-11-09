package com.allforland.orda.domain.trail.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 상세지점-등산로 구간 매칭 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SpotMatchRow {
    private final Long edgeId;
    private final BigDecimal distanceM;
    private final BigDecimal positionRatio;
    private final String matchType;
    private final String matchStatus;
}
