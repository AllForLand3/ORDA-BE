package com.allforland.orda.domain.analysis.mapper;

import com.allforland.orda.domain.analysis.vo.DemAssetRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** DEM 메타데이터를 조회하는 MyBatis 매퍼다. 사용 가능 여부는 호출하는 Service에서 판단한다. */
@Mapper
public interface DemAssetMapper {
    /** 존재하지 않으면 null을 반환하며 비활성·전처리 실패 행도 상태와 함께 반환한다. */
    DemAssetRow findById(@Param("demId") long demId);
}
