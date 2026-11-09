package com.allforland.orda.domain.analysis.terrain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TerrainAnalysisResponse {

    private final String analysisId;
    private final String courseId;
    private final int revNo;
    private final String demId;
    private final String demName;
    private final BigDecimal resolutionM;
    private final BigDecimal sampleIntervalM;
    private final String status;

    private final Summary analysis;

    // 요약 조회는 null로 전달하여 필드 자체를 제외한다.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<Sample> samples;

    private final Instant completedAt;
    private final Failure failure;

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Summary {

        private final BigDecimal ascentM;
        private final BigDecimal descentM;
        private final BigDecimal maxElevationM;
        private final BigDecimal minElevationM;
        private final int sampleCount;
        private final int missingCount;
    }

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Sample {

        private final int seqNo;
        private final BigDecimal distanceM;
        private final BigDecimal elevationM;
    }

    @Getter
    @AllArgsConstructor
    public static class Failure {

        private final String code;
        private final String message;
    }
}
