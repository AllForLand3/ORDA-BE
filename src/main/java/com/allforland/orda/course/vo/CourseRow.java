package com.allforland.orda.course.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

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
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Boolean geometryValid;
}
