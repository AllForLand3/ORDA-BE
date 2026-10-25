package com.allforland.orda.domain.analysis.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** DEM 파일 메타데이터 조회 결과다. 내부 파일 경로를 포함하므로 API 응답으로 사용하지 않는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class DemAssetRow {
    private final Long demId;
    private final Long datasetId;
    private final String demName;
    private final String filePath;
    private final String crsCode;
    private final BigDecimal resolutionM;
    private final String surfaceTypeCode;
    private final BigDecimal noDataValue;
    private final String prepStatusCode;
    private final String useYn;
}
