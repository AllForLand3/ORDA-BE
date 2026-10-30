package com.allforland.orda.domain.analysis.mapper;

import com.allforland.orda.domain.analysis.vo.CourseAnalysisSourceRow;
import com.allforland.orda.domain.analysis.vo.CourseSamplePoint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseAnalysisMapper {

    /** 삭제되지 않은 소유자 코스의 분석용 스냅샷을 조회한다. 코스가 없거나 다른 사용자 소유이면 null을 반환한다. */
    CourseAnalysisSourceRow findSource(
            @Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    /** 검증한 형상 스냅샷에서 거리순 EPSG:4326 좌표를 생성한다. */
    List<CourseSamplePoint> generateSamplePoints(
            @Param("geometryHex") String geometryHex, @Param("distances") List<Double> distances);
}
