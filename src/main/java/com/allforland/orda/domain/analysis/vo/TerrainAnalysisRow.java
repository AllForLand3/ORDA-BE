package com.allforland.orda.domain.analysis.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 분석 실행 조건과 현재 상태를 조회한 결과다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class TerrainAnalysisRow {

    private final Long analysisId;
    private final Long courseId;
    private final Integer revNo;
    private final Long demId;
    private final BigDecimal sampleIntervalM;
    private final String statusCode;
}
