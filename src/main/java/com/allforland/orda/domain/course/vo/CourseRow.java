package com.allforland.orda.domain.course.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 조회 결과를 담는 데이터 객체다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class CourseRow {
    private final Long courseId;
    private final Long mountainId;
    private final String courseName;
    private final String memo;
    private final String createType;
    private final Integer revNo;
    private final String geometryJson;
    private final BigDecimal lengthM;
    private final String rowVersion;
    private final Boolean geometryValid;
}
