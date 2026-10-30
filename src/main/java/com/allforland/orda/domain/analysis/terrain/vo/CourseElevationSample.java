package com.allforland.orda.domain.analysis.terrain.vo;

import com.allforland.orda.domain.analysis.dem.vo.ElevationResult;
import com.allforland.orda.domain.analysis.dem.vo.ElevationStatus;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 거리순 코스 샘플의 좌표와 고도 판독 결과다. 결측 고도는 null로 유지한다. */
@Getter
@Accessors(fluent = true)
public final class CourseElevationSample {

    private final int seqNo;
    private final double distanceM;
    private final double longitude;
    private final double latitude;
    private final ElevationStatus status;
    private final Double elevationM;

    public CourseElevationSample(CourseSamplePoint point, ElevationResult elevation) {
        this.seqNo = point.seqNo();
        this.distanceM = point.distanceM();
        this.longitude = point.longitude();
        this.latitude = point.latitude();
        this.status = elevation.status();
        this.elevationM = elevation.elevationM();
    }
}
