package com.allforland.orda.domain.analysis.terrain.mapper;

import com.allforland.orda.domain.analysis.terrain.vo.TrailEdgeElevationSamplePoint;
import com.allforland.orda.domain.analysis.terrain.vo.TrailEdgeElevationUpdate;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** DEM 기준 구간 고도 백필 대상 좌표를 만들고 계산 결과를 저장한다. */
@Mapper
public interface TrailEdgeElevationBackfillMapper {

    int countCandidates();

    List<TrailEdgeElevationSamplePoint> findSamplePoints(
            @Param("sampleIntervalM") BigDecimal sampleIntervalM);

    int updateAll(@Param("rows") List<TrailEdgeElevationUpdate> rows);
}
