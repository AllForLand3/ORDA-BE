package com.allforland.orda.domain.analysis.terrain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 저장 정밀도로 변환한 거리별 고도다. 결측 고도는 null이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class ElevationSampleSaveRow {

    private final int seqNo;
    private final BigDecimal distanceM;
    private final BigDecimal elevationM;
}
