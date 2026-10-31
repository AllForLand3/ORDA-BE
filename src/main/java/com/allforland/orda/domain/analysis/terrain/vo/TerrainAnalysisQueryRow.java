package com.allforland.orda.domain.analysis.terrain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 코스·분석·DEM·샘플을 같은 SQL 스냅샷으로 조회한 행이다. */
@Getter
@Setter
@NoArgsConstructor
public class TerrainAnalysisQueryRow {

    private Long courseId;
    private Integer currentRevNo;

    private Long analysisId;
    private Integer revNo;
    private Long demId;
    private String demName;
    private BigDecimal resolutionM;
    private BigDecimal sampleIntervalM;
    private String status;

    private BigDecimal ascentM;
    private BigDecimal descentM;
    private BigDecimal maxElevationM;
    private BigDecimal minElevationM;
    private Integer sampleCount;
    private Integer missingCount;

    private LocalDateTime completedAt;
    private String errorMessage;

    private Integer seqNo;
    private BigDecimal distanceM;
    private BigDecimal elevationM;
}
