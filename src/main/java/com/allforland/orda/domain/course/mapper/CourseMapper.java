package com.allforland.orda.domain.course.mapper;

import com.allforland.orda.domain.course.dto.CourseListItemResponse;
import com.allforland.orda.domain.course.vo.CourseGeometryRow;
import com.allforland.orda.domain.course.vo.CourseRow;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 코스 데이터 조회와 변경을 위한 MyBatis 매퍼다. */
@Mapper
public interface CourseMapper {
    long countCourses(@Param("ownerUserId") long ownerUserId);

    List<CourseListItemResponse> findCourses(
            @Param("ownerUserId") long ownerUserId,
            @Param("limit") int limit,
            @Param("offset") long offset);

    boolean activeMountainExists(@Param("mountainId") long mountainId);

    CourseRow findCourse(
            @Param("courseId") long courseId,
            @Param("ownerUserId") long ownerUserId,
            @Param("lock") boolean lock);

    Long insertCourse(
            @Param("mountainId") long mountainId,
            @Param("ownerUserId") long ownerUserId,
            @Param("courseName") String courseName,
            @Param("memo") String memo,
            @Param("createType") String createType,
            @Param("geometryHex") String geometryHex,
            @Param("lengthM") BigDecimal lengthM);

    int updateCourse(
            @Param("courseId") long courseId,
            @Param("ownerUserId") long ownerUserId,
            @Param("courseName") String courseName,
            @Param("memo") String memo,
            @Param("geometryChanged") boolean geometryChanged,
            @Param("geometryHex") String geometryHex,
            @Param("lengthM") BigDecimal lengthM);

    int deleteCourse(@Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    CourseGeometryRow prepareGeometry(
            @Param("geometryJson") String geometryJson,
            @Param("mountainId") long mountainId,
            @Param("courseId") Long courseId);
}
