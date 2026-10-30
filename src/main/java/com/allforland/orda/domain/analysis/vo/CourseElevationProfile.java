package com.allforland.orda.domain.analysis.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 특정 코스 버전의 고도 프로파일과 유효 관측 구간의 요약 통계다. */
@Getter
@Accessors(fluent = true)
public final class CourseElevationProfile {

    private final long courseId;
    private final int revNo;
    private final long demId;
    private final double actualLengthM;
    private final BigDecimal sampleIntervalM;
    private final List<CourseElevationSample> samples;

    private final double minElevationM;
    private final double maxElevationM;
    private final double ascentM;
    private final double descentM;

    private final int totalSampleCount;
    private final int validSampleCount;
    private final int missingSampleCount;
    private final int validSegmentCount;

    public CourseElevationProfile(
            long courseId,
            int revNo,
            long demId,
            double actualLengthM,
            BigDecimal sampleIntervalM,
            List<CourseElevationSample> samples,
            double minElevationM,
            double maxElevationM,
            double ascentM,
            double descentM,
            int validSegmentCount) {
        this.courseId = courseId;
        this.revNo = revNo;
        this.demId = demId;
        this.actualLengthM = actualLengthM;
        this.sampleIntervalM = sampleIntervalM;
        this.samples = List.copyOf(samples);

        this.minElevationM = minElevationM;
        this.maxElevationM = maxElevationM;
        this.ascentM = ascentM;
        this.descentM = descentM;

        this.totalSampleCount = this.samples.size();
        this.validSampleCount =
                (int)
                        this.samples.stream()
                                .filter(sample -> sample.status() == ElevationStatus.VALID)
                                .count();
        this.missingSampleCount = totalSampleCount - validSampleCount;
        this.validSegmentCount = validSegmentCount;
    }
}
