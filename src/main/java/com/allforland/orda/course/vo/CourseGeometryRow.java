package com.allforland.orda.course.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 공간 연산 결과를 담는 데이터 객체다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class CourseGeometryRow {
    private final String geometryHex;
    private final BigDecimal lengthM;
    private final Boolean valid;
    private final Boolean areaAvailable;
    private final Boolean covered;
    private final Boolean sameGeometry;
}
