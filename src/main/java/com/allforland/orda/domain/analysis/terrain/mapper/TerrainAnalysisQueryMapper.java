package com.allforland.orda.domain.analysis.terrain.mapper;

import com.allforland.orda.domain.analysis.terrain.vo.TerrainAnalysisQueryRow;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TerrainAnalysisQueryMapper {

    /** 소유 코스의 현재 버전·지정 조건 분석을 조회한다. */
    List<TerrainAnalysisQueryRow> findCurrent(
            @Param("courseId") long courseId,
            @Param("ownerUserId") long ownerUserId,
            @Param("demId") long demId,
            @Param("sampleIntervalM") BigDecimal sampleIntervalM,
            @Param("includeSamples") boolean includeSamples);

    /** 이전 버전도 허용하되 현재 코스 소유권과 삭제 여부를 확인한다. */
    List<TerrainAnalysisQueryRow> findOwnedById(
            @Param("analysisId") long analysisId, @Param("ownerUserId") long ownerUserId);
}
