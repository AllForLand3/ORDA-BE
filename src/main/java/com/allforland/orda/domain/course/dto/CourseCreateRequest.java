package com.allforland.orda.domain.course.dto;

import com.allforland.orda.common.geo.LineStringGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 코스 등록 요청 값을 담는다. */
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CourseCreateRequest {
    private String mountainId;

    private String courseName;

    private String memo;

    private String createType;

    private LineStringGeometry geometry;
}
