package com.allforland.orda.domain.analysis.vo;

import lombok.Getter;
import lombok.experimental.Accessors;

/** 분석 요청의 실행 여부와 대상 분석을 나타낸다. */
@Getter
@Accessors(fluent = true)
public final class TerrainAnalysisPreparation {

    private final long analysisId;
    private final PreparedTerrainAnalysis prepared;

    private TerrainAnalysisPreparation(long analysisId, PreparedTerrainAnalysis prepared) {
        this.analysisId = analysisId;
        this.prepared = prepared;
    }

    public static TerrainAnalysisPreparation execute(PreparedTerrainAnalysis prepared) {
        return new TerrainAnalysisPreparation(prepared.analysisId(), prepared);
    }

    public static TerrainAnalysisPreparation reuse(long analysisId) {
        return new TerrainAnalysisPreparation(analysisId, null);
    }

    public boolean shouldExecute() {
        return prepared != null;
    }
}
