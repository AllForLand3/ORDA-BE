package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import lombok.Getter;

@Getter
public class TerrainAnalysisExecutionException extends BusinessException {

    private final long analysisId;

    public TerrainAnalysisExecutionException(
        long analysisId, RuntimeException cause) {
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
