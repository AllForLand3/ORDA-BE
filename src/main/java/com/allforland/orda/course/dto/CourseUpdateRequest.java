package com.allforland.orda.course.dto;

import com.allforland.orda.common.geo.LineStringGeometry;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class CourseUpdateRequest {
    private String courseName;
    private String memo;
    private LineStringGeometry geometry;
    private boolean namePresent;
    private boolean memoPresent;
    private boolean geometryPresent;

    @JsonSetter("courseName")
    public void setCourseName(String value) {
        namePresent = true;
        courseName = value;
    }

    @JsonSetter("memo")
    public void setMemo(String value) {
        memoPresent = true;
        memo = value;
    }

    @JsonSetter("geometry")
    public void setGeometry(LineStringGeometry value) {
        geometryPresent = true;
        geometry = value;
    }
}
