package com.allforland.orda.domain.course.dto;

import com.allforland.orda.common.geo.LineStringGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 코스 상세 조회 응답 값을 담는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
    "courseId",
    "mountainId",
    "courseName",
    "memo",
    "createType",
    "revNo",
    "geometry",
    "lengthM"
})
public class CourseDetailResponse {
    private final String courseId;
    private final String mountainId;
    private final String courseName;
    private final String memo;
    private final String createType;
    private final int revNo;
    private final LineStringGeometry geometry;
    private final BigDecimal lengthM;
    @JsonIgnore private final String rowVersion;
}
