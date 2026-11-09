package com.allforland.orda.domain.analysis.terrain.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 샘플링에 실제 사용한 코스 버전·조건과 거리순 좌표 목록이다. API 응답 DTO와 구분한다. */
@Getter
@Accessors(fluent = true)
public final class CourseSamplingResult {
    private final long courseId;
    private final int revNo;
    private final double actualLengthM;
    private final BigDecimal sampleIntervalM;
    private final List<CourseSamplePoint> samples;

    public CourseSamplingResult(
            long courseId,
            int revNo,
            double actualLengthM,
            BigDecimal sampleIntervalM,
            List<CourseSamplePoint> samples) {
        this.courseId = courseId;
        this.revNo = revNo;
        this.actualLengthM = actualLengthM;
        this.sampleIntervalM = sampleIntervalM;
        this.samples = List.copyOf(samples);
    }
}
