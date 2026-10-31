package com.allforland.orda.domain.analysis.terrain.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 저장 정밀도로 변환한 분석 요약 통계다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class TerrainAnalysisSummarySaveRow {

    private final BigDecimal ascentM;
    private final BigDecimal descentM;
    private final BigDecimal maxElevationM;
    private final BigDecimal minElevationM;
    private final int sampleCount;
    private final int missingCount;
}
