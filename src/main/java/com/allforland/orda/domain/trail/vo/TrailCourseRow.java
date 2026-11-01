package com.allforland.orda.domain.trail.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 주변 조회 기준이 되는 소유자 코스 행이다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class TrailCourseRow {
    private final Long courseId;
    private final Long mountainId;
    private final Integer revNo;
    private final Boolean geometryValid;
}
