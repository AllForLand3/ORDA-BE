package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.dem.service.ElevationService;
import com.allforland.orda.domain.analysis.dem.vo.ElevationResult;
import com.allforland.orda.domain.analysis.dem.vo.ElevationStatus;
import com.allforland.orda.domain.analysis.terrain.vo.CourseElevationProfile;
import com.allforland.orda.domain.analysis.terrain.vo.CourseElevationSample;
import com.allforland.orda.domain.analysis.terrain.vo.CourseSamplePoint;
import com.allforland.orda.domain.analysis.terrain.vo.CourseSamplingResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 코스 샘플의 고도를 판독하고 프로파일과 요약 통계를 계산한다. */
@Service
@RequiredArgsConstructor
public class CourseElevationProfileService {

    private final CourseSamplingService courseSamplingService;
    private final ElevationService elevationService;

    /** 코스 버전을 확인하고 좌표를 생성한 뒤 고도 프로파일을 계산한다. */
    public CourseElevationProfile calculate(
            long courseId, int expectedRevNo, long demId, BigDecimal requestedIntervalM) {
        if (demId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        CourseSamplingResult sampling =
                courseSamplingService.sample(courseId, expectedRevNo, requestedIntervalM);

        return calculate(sampling, demId);
    }

    /** 소유권과 버전 검증을 거쳐 확보한 내부 샘플 스냅샷으로 계산한다. 코스 조회와 좌표 생성을 다시 수행하지 않는다. */
    public CourseElevationProfile calculate(CourseSamplingResult sampling, long demId) {
        if (sampling == null || demId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        List<CourseElevationSample> samples = new ArrayList<>(sampling.samples().size());

        Double minElevationM = null;
        Double maxElevationM = null;
        Double previousElevationM = null;

        double ascentM = 0.0;
        double descentM = 0.0;
        int validSegmentCount = 0;

        for (CourseSamplePoint point : sampling.samples()) {
            ElevationResult elevation =
                    elevationService.extract(demId, point.longitude(), point.latitude());

            samples.add(new CourseElevationSample(point, elevation));

            if (elevation.status() != ElevationStatus.VALID) {
                // 결측 구간을 건너뛰어 상승·하강을 계산하지 않는다.
                previousElevationM = null;
                continue;
            }

            double currentElevationM = elevation.elevationM();

            if (minElevationM == null) {
                minElevationM = currentElevationM;
                maxElevationM = currentElevationM;
            } else {
                minElevationM = Math.min(minElevationM, currentElevationM);
                maxElevationM = Math.max(maxElevationM, currentElevationM);
            }

            if (previousElevationM != null) {
                double differenceM = currentElevationM - previousElevationM;

                if (differenceM > 0.0) {
                    ascentM += differenceM;
                } else if (differenceM < 0.0) {
                    descentM -= differenceM;
                }

                validSegmentCount++;
            }

            previousElevationM = currentElevationM;
        }

        if (minElevationM == null) {
            throw new BusinessException(ErrorCode.ANALYSIS_NO_VALID_ELEVATION);
        }

        if (validSegmentCount == 0) {
            throw new BusinessException(ErrorCode.ANALYSIS_INSUFFICIENT_VALID_SEGMENTS);
        }

        return new CourseElevationProfile(
                sampling.courseId(),
                sampling.revNo(),
                demId,
                sampling.actualLengthM(),
                sampling.sampleIntervalM(),
                samples,
                minElevationM,
                maxElevationM,
                ascentM,
                descentM,
                validSegmentCount);
    }
}
