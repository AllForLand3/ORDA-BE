package com.allforland.orda.domain.analysis.terrain.dto;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
public class TerrainAnalysisRequest {

    private final String demId;
    private final int revNo;
    private final BigDecimal sampleIntervalM;

    private TerrainAnalysisRequest(
        String demId, int revNo, BigDecimal sampleIntervalM) {
        this.demId = demId;
        this.revNo = revNo;
        this.sampleIntervalM = sampleIntervalM;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TerrainAnalysisRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        JsonNode demId = body.get("demId");
        JsonNode revNo = body.get("revNo");
        JsonNode interval = body.get("sampleIntervalM");

        if (demId == null || !demId.isTextual()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (revNo == null
            || !revNo.isIntegralNumber()
            || !revNo.canConvertToInt()
            || revNo.intValue() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        BigDecimal sampleIntervalM = null;

        if (interval != null && !interval.isNull()) {
            if (!interval.isNumber()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }

            sampleIntervalM = interval.decimalValue();
        }

        return new TerrainAnalysisRequest(
            demId.textValue(), revNo.intValue(), sampleIntervalM);
    }
}
