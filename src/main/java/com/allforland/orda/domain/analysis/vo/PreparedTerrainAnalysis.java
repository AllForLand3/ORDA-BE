package com.allforland.orda.domain.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 실행 권한을 확보한 분석과 준비 시점의 코스 샘플 스냅샷이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class PreparedTerrainAnalysis {

    private final long analysisId;
    private final long ownerUserId;
    private final long demId;
    private final CourseSamplingResult sampling;
}
