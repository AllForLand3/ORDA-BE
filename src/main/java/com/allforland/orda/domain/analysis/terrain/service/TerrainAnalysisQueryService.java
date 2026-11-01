package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse.Failure;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse.Sample;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse.Summary;
import com.allforland.orda.domain.analysis.terrain.mapper.TerrainAnalysisQueryMapper;
import com.allforland.orda.domain.analysis.terrain.vo.TerrainAnalysisQueryRow;
import com.allforland.orda.domain.auth.service.AuthService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TerrainAnalysisQueryService {

    private static final Set<ErrorCode> PUBLIC_FAILURE_CODES =
            Set.of(
                    ErrorCode.ANALYSIS_NO_VALID_ELEVATION,
                    ErrorCode.ANALYSIS_INSUFFICIENT_VALID_SEGMENTS,
                    ErrorCode.DEM_UNAVAILABLE,
                    ErrorCode.DEM_READ_FAILED,
                    ErrorCode.SERVICE_UNAVAILABLE,
                    ErrorCode.INTERNAL_SERVER_ERROR);

    private final TerrainAnalysisQueryMapper mapper;
    private final AuthService authService;
    private final CourseSampleDistanceGenerator distanceGenerator;
    private final ZoneId storedTimeZone;

    public TerrainAnalysisQueryService(
            TerrainAnalysisQueryMapper mapper,
            AuthService authService,
            CourseSampleDistanceGenerator distanceGenerator,
            @Value("${orda.analysis.result-time-zone}") String storedTimeZone) {

        this.mapper = mapper;
        this.authService = authService;
        this.distanceGenerator = distanceGenerator;
        this.storedTimeZone = ZoneId.of(storedTimeZone);
    }

    /** 현재 버전의 요약만 반환한다. */
    @Transactional(readOnly = true)
    public TerrainAnalysisResponse findCurrentSummary(
            long courseId, long demId, BigDecimal requestedIntervalM, Integer expectedRevNo) {

        return findCurrent(courseId, demId, requestedIntervalM, expectedRevNo, false);
    }

    /** 현재 버전의 요약과 전체 샘플을 반환한다. */
    @Transactional(readOnly = true)
    public TerrainAnalysisResponse findCurrentProfile(
            long courseId, long demId, BigDecimal requestedIntervalM, Integer expectedRevNo) {

        return findCurrent(courseId, demId, requestedIntervalM, expectedRevNo, true);
    }

    /** 이전 버전도 허용하되 현재 코스 접근 권한을 확인한다. */
    @Transactional(readOnly = true)
    public TerrainAnalysisResponse findOwned(long analysisId) {

        if (analysisId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        long ownerUserId = authService.requireCurrentUser().userId();

        List<TerrainAnalysisQueryRow> rows = mapper.findOwnedById(analysisId, ownerUserId);

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        return toResponse(rows, true);
    }

    private TerrainAnalysisResponse findCurrent(
            long courseId,
            long demId,
            BigDecimal requestedIntervalM,
            Integer expectedRevNo,
            boolean includeSamples) {

        if (courseId <= 0 || demId <= 0 || (expectedRevNo != null && expectedRevNo <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        long ownerUserId = authService.requireCurrentUser().userId();

        BigDecimal interval = distanceGenerator.normalizeInterval(requestedIntervalM);

        List<TerrainAnalysisQueryRow> rows =
                mapper.findCurrent(courseId, ownerUserId, demId, interval, includeSamples);

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        TerrainAnalysisQueryRow first = rows.get(0);

        if (expectedRevNo != null && !expectedRevNo.equals(first.getCurrentRevNo())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        if (first.getAnalysisId() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        return toResponse(rows, includeSamples);
    }

    private TerrainAnalysisResponse toResponse(
            List<TerrainAnalysisQueryRow> rows, boolean includeSamples) {

        TerrainAnalysisQueryRow first = rows.get(0);

        Summary summary = null;
        List<Sample> samples = includeSamples ? List.of() : null;
        Instant completedAt = null;
        Failure failure = null;

        String status = first.getStatus();

        if (status == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        switch (status) {
            case "DONE" -> {
                validateSummary(first);

                summary =
                        new Summary(
                                first.getAscentM(),
                                first.getDescentM(),
                                first.getMaxElevationM(),
                                first.getMinElevationM(),
                                first.getSampleCount(),
                                first.getMissingCount());

                completedAt = first.getCompletedAt().atZone(storedTimeZone).toInstant();

                if (includeSamples) {
                    validateSamples(rows, first);

                    samples =
                            rows.stream()
                                    .map(
                                            row ->
                                                    new Sample(
                                                            row.getSeqNo(),
                                                            row.getDistanceM(),
                                                            row.getElevationM()))
                                    .toList();
                }
            }
            case "REQUESTED", "RUNNING" -> {
                // 통계·샘플·완료 시각·실패 정보는 반환하지 않는다.
            }
            case "FAILED" -> failure = safeFailure(first.getErrorMessage());
            default -> throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return new TerrainAnalysisResponse(
                first.getAnalysisId().toString(),
                first.getCourseId().toString(),
                first.getRevNo(),
                first.getDemId().toString(),
                first.getDemName(),
                first.getResolutionM(),
                first.getSampleIntervalM(),
                status,
                summary,
                samples,
                completedAt,
                failure);
    }

    private void validateSummary(TerrainAnalysisQueryRow row) {

        if (row.getAscentM() == null
                || row.getDescentM() == null
                || row.getMaxElevationM() == null
                || row.getMinElevationM() == null
                || row.getSampleCount() == null
                || row.getMissingCount() == null
                || row.getCompletedAt() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int count = row.getSampleCount();
        int missing = row.getMissingCount();

        if (count < 2
                || count > 2000
                || missing < 0
                || missing > count
                || row.getAscentM().signum() < 0
                || row.getDescentM().signum() < 0
                || row.getMinElevationM().compareTo(row.getMaxElevationM()) > 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateSamples(
            List<TerrainAnalysisQueryRow> rows, TerrainAnalysisQueryRow first) {

        if (rows.size() != first.getSampleCount()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int missing = 0;
        BigDecimal previousDistance = null;

        for (int index = 0; index < rows.size(); index++) {
            TerrainAnalysisQueryRow row = rows.get(index);

            if (row.getSeqNo() == null
                    || row.getSeqNo() != index + 1
                    || row.getDistanceM() == null
                    || row.getDistanceM().signum() < 0) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            if (index == 0 && row.getDistanceM().signum() != 0) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            // DB는 거리를 소수점 한 자리로 저장하므로,
            // 반올림으로 같은 거리가 될 수 있어 감소 여부만 검사한다.
            if (previousDistance != null && row.getDistanceM().compareTo(previousDistance) < 0) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            previousDistance = row.getDistanceM();

            if (row.getElevationM() == null) {
                missing++;
            }
        }

        if (missing != first.getMissingCount()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Failure safeFailure(String storedMessage) {

        if (storedMessage != null) {
            int separator = storedMessage.indexOf(':');

            String storedCode =
                    (separator < 0 ? storedMessage : storedMessage.substring(0, separator)).trim();

            for (ErrorCode allowed : PUBLIC_FAILURE_CODES) {
                if (allowed.name().equals(storedCode)) {
                    return new Failure(allowed.name(), allowed.getMessage());
                }
            }
        }

        return new Failure(ErrorCode.INTERNAL_SERVER_ERROR.name(), "고도 분석에 실패했습니다. 다시 요청해주세요.");
    }
}
