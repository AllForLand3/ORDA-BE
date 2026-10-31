package com.allforland.orda.common.error;

import lombok.Getter;

/** 기존 오류 코드와 분석 결과 조회에 필요한 식별자를 함께 전달한다. */
@Getter
public class TerrainAnalysisExecutionException extends BusinessException {

    private final long analysisId;

    public TerrainAnalysisExecutionException(long analysisId, RuntimeException cause) {
        super(resolveCode(cause));
        this.analysisId = analysisId;
        initCause(cause);
    }

    private static ErrorCode resolveCode(RuntimeException cause) {
        if (cause instanceof BusinessException business) {
            return business.getErrorCode();
        }

        return ErrorCode.INTERNAL_SERVER_ERROR;
    }
}
