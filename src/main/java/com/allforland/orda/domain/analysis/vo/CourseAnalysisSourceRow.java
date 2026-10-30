package com.allforland.orda.domain.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 한 번의 조회에서 확보한 분석 대상 코스의 형상·버전·실제 길이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class CourseAnalysisSourceRow {

    private final Long courseId;
    private final Integer revNo;
    private final String geometryHex;
    private final Double actualLengthM;
    private final Boolean geometryValid;
}
