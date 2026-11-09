package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.terrain.mapper.CourseAnalysisMapper;
import com.allforland.orda.domain.analysis.terrain.vo.CourseAnalysisSourceRow;
import com.allforland.orda.domain.analysis.terrain.vo.CourseSamplePoint;
import com.allforland.orda.domain.analysis.terrain.vo.CourseSamplingResult;
import com.allforland.orda.domain.auth.service.AuthService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 현재 사용자의 특정 코스 버전을 읽어 거리순 EPSG:4326 샘플 좌표를 생성한다. */
@Service
@RequiredArgsConstructor
public class CourseSamplingService {
    private final AuthService authService;
    private final CourseAnalysisMapper mapper;
    private final CourseSampleDistanceGenerator distanceGenerator;

    /** 고도 판독·저장 없이 하나의 형상 스냅샷에서만 좌표를 생성한다. */
    @Transactional(readOnly = true)
    public CourseSamplingResult sample(
            long courseId, int expectedRevNo, BigDecimal requestedIntervalM) {
        if (courseId <= 0 || expectedRevNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        long owner = authService.requireCurrentUser().userId();
        BigDecimal interval = distanceGenerator.normalizeInterval(requestedIntervalM);
        CourseAnalysisSourceRow source = mapper.findSource(courseId, owner);
        if (source == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
        validateSource(source, courseId);
        if (source.revNo() != expectedRevNo) {
            // 분석 API 계약 확정 전에는 기존 상태 충돌 코드를 사용한다.
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        List<Double> distances = distanceGenerator.generate(source.actualLengthM(), interval);
        List<CourseSamplePoint> samples =
                mapper.generateSamplePoints(source.geometryHex(), distances);
        validateSamples(samples, distances);
        return new CourseSamplingResult(
                source.courseId(), source.revNo(), source.actualLengthM(), interval, samples);
    }

    private void validateSource(CourseAnalysisSourceRow source, long courseId) {
        if (source.courseId() == null
                || source.courseId() != courseId
                || source.revNo() == null
                || source.revNo() <= 0
                || !Boolean.TRUE.equals(source.geometryValid())
                || source.geometryHex() == null
                || source.geometryHex().isBlank()
                || source.actualLengthM() == null
                || !Double.isFinite(source.actualLengthM())
                || source.actualLengthM() <= 0) {
            throw new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
        }
    }

    /** SQL 매핑·정렬 오류로 샘플이 누락되거나 잘못 연결되는 것을 거부한다. */
    private void validateSamples(List<CourseSamplePoint> samples, List<Double> distances) {
        if (samples == null || samples.size() != distances.size()) {
            throw new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
        }
        for (int index = 0; index < samples.size(); index++) {
            CourseSamplePoint point = samples.get(index);
            if (point == null
                    || point.seqNo() != index + 1
                    || !Double.isFinite(point.distanceM())
                    || Double.compare(point.distanceM(), distances.get(index)) != 0
                    || (index > 0 && point.distanceM() <= samples.get(index - 1).distanceM())
                    || !Double.isFinite(point.longitude())
                    || point.longitude() < -180
                    || point.longitude() > 180
                    || !Double.isFinite(point.latitude())
                    || point.latitude() < -90
                    || point.latitude() > 90) {
                throw new BusinessException(ErrorCode.COURSE_STATE_UNAVAILABLE);
            }
        }
    }
}
