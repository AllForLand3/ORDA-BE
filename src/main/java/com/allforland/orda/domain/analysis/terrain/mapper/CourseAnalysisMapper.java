package com.allforland.orda.domain.analysis.terrain.mapper;

import com.allforland.orda.domain.analysis.terrain.vo.CourseAnalysisSourceRow;
import com.allforland.orda.domain.analysis.terrain.vo.CourseSamplePoint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseAnalysisMapper {

    /** 준비 트랜잭션 동안 소유자 코스의 변경·삭제를 막는다. */
    Long lockOwnedCourse(@Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    /** 삭제되지 않은 소유자 코스의 분석용 스냅샷을 조회한다. 코스가 없거나 다른 사용자 소유이면 null을 반환한다. */
    CourseAnalysisSourceRow findSource(
            @Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    /** 검증한 형상 스냅샷에서 거리순 EPSG:4326 좌표를 생성한다. */
    List<CourseSamplePoint> generateSamplePoints(
            @Param("geometryHex") String geometryHex, @Param("distances") List<Double> distances);
}
