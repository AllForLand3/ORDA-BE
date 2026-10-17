package com.allforland.orda.course.mapper;

import com.allforland.orda.course.vo.CourseGeometryRow;
import com.allforland.orda.course.vo.CourseRow;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CourseMapper {
    boolean activeUserExists(@Param("userId") long userId);

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
            @Param("lengthM") BigDecimal lengthM,
            @Param("actorId") String actorId);

    int updateCourse(
            @Param("courseId") long courseId,
            @Param("ownerUserId") long ownerUserId,
            @Param("courseName") String courseName,
            @Param("memo") String memo,
            @Param("geometryChanged") boolean geometryChanged,
            @Param("geometryHex") String geometryHex,
            @Param("lengthM") BigDecimal lengthM,
            @Param("actorId") String actorId);

    int deleteCourse(
            @Param("courseId") long courseId,
            @Param("ownerUserId") long ownerUserId,
            @Param("actorId") String actorId);

    CourseGeometryRow prepareGeometry(
            @Param("geometryJson") String geometryJson,
            @Param("mountainId") long mountainId,
            @Param("courseId") Long courseId);
}
