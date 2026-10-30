package com.allforland.orda.domain.analysis.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.mapper.DemAssetMapper;
import com.allforland.orda.domain.analysis.mapper.TerrainAnalysisMapper;
import com.allforland.orda.domain.analysis.vo.CourseElevationProfile;
import com.allforland.orda.domain.analysis.vo.CourseSamplingResult;
import com.allforland.orda.domain.analysis.vo.DemAssetRow;
import com.allforland.orda.domain.analysis.vo.ElevationSampleSaveRow;
import com.allforland.orda.domain.analysis.vo.ElevationStatus;
import com.allforland.orda.domain.analysis.vo.PreparedTerrainAnalysis;
import com.allforland.orda.domain.analysis.vo.TerrainAnalysisRow;
import com.allforland.orda.domain.analysis.vo.TerrainAnalysisSummarySaveRow;
import com.allforland.orda.domain.auth.service.AuthService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 분석 준비·결과 저장·실패 기록을 각각 짧은 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class TerrainAnalysisTransactionService {

    private final AuthService authService;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final CourseSamplingService samplingService;
    private final CourseSampleDistanceGenerator distanceGenerator;
    private final DemAssetMapper demMapper;
    private final TerrainAnalysisMapper analysisMapper;

    /** 코스 스냅샷을 확보하고 신규 분석의 실행 권한을 얻는다. TIFF는 읽지 않는다. */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public PreparedTerrainAnalysis prepareAndStart(
            long courseId, int expectedRevNo, long demId, BigDecimal requestedIntervalM) {
        if (courseId <= 0 || expectedRevNo <= 0 || demId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        long ownerUserId = authService.requireCurrentUser().userId();
        BigDecimal interval = distanceGenerator.normalizeInterval(requestedIntervalM);

        // 기존 XML의 잠금 SQL을 사용해 Mapper 인터페이스를 변경하지 않는다.
        Long lockedCourseId =
                sqlSessionTemplate.selectOne(
                        "com.allforland.orda.domain.analysis.mapper.CourseAnalysisMapper.lockOwnedCourse",
                        Map.of("courseId", courseId, "ownerUserId", ownerUserId));

        if (lockedCourseId == null) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }

        CourseSamplingResult sampling = samplingService.sample(courseId, expectedRevNo, interval);

        validateDem(demId);

        Long analysisId =
                analysisMapper.insertRequested(
                        sampling.courseId(), sampling.revNo(), demId, sampling.sampleIntervalM());

        if (analysisId == null) {
            // 동일 조건의 재사용·재시도 정책은 후속 단계에서 연결한다.
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        if (analysisMapper.markRunning(analysisId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        return new PreparedTerrainAnalysis(analysisId, ownerUserId, demId, sampling);
    }

    /** 샘플·요약 통계·완료 상태를 하나의 트랜잭션으로 저장한다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void complete(PreparedTerrainAnalysis prepared, CourseElevationProfile profile) {
        validatePreparedOwner(prepared);

        TerrainAnalysisRow row = lockMatchingAnalysis(prepared);

        if (!"RUNNING".equals(row.statusCode())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        List<ElevationSampleSaveRow> samples = validateAndConvertSamples(prepared, profile);

        TerrainAnalysisSummarySaveRow summary =
                new TerrainAnalysisSummarySaveRow(
                        decimal(profile.ascentM(), 7, 1),
                        decimal(profile.descentM(), 7, 1),
                        decimal(profile.maxElevationM(), 7, 2),
                        decimal(profile.minElevationM(), 7, 2),
                        profile.totalSampleCount(),
                        profile.missingSampleCount());

        int inserted = analysisMapper.insertSamples(prepared.analysisId(), samples);

        if (inserted != samples.size()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (analysisMapper.markDone(prepared.analysisId(), summary) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    /** 계산·저장 실패를 독립된 트랜잭션으로 기록한다. 이미 종료된 분석은 변경하지 않는다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean fail(PreparedTerrainAnalysis prepared, RuntimeException failure) {
        if (failure == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        validatePreparedOwner(prepared);

        TerrainAnalysisRow row = lockMatchingAnalysis(prepared);

        if (!"RUNNING".equals(row.statusCode())) {
            return false;
        }

        ErrorCode code =
                failure instanceof BusinessException business
                        ? business.getErrorCode()
                        : ErrorCode.INTERNAL_SERVER_ERROR;

        String safeMessage = code.name() + ": " + code.getMessage();

        if (analysisMapper.markFailed(prepared.analysisId(), safeMessage) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        return true;
    }

    private void validateDem(long demId) {
        DemAssetRow dem = demMapper.findById(demId);

        if (dem == null) {
            throw new BusinessException(ErrorCode.DEM_NOT_FOUND);
        }

        if (!"Y".equals(dem.useYn())
                || (!"RAW".equals(dem.prepStatusCode()) && !"READY".equals(dem.prepStatusCode()))) {
            throw new BusinessException(ErrorCode.DEM_UNAVAILABLE);
        }
    }

    private void validatePreparedOwner(PreparedTerrainAnalysis prepared) {
        if (prepared == null
                || prepared.analysisId() <= 0
                || prepared.ownerUserId() <= 0
                || prepared.demId() <= 0
                || prepared.sampling() == null
                || prepared.sampling().courseId() <= 0
                || prepared.sampling().revNo() <= 0
                || prepared.sampling().sampleIntervalM() == null
                || !Double.isFinite(prepared.sampling().actualLengthM())
                || prepared.sampling().actualLengthM() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (authService.requireCurrentUser().userId() != prepared.ownerUserId()) {
            throw new BusinessException(ErrorCode.COURSE_NOT_FOUND);
        }
    }

    private TerrainAnalysisRow lockMatchingAnalysis(PreparedTerrainAnalysis prepared) {
        TerrainAnalysisRow row = analysisMapper.findByIdForUpdate(prepared.analysisId());

        CourseSamplingResult sampling = prepared.sampling();

        if (row == null
                || !Objects.equals(row.analysisId(), prepared.analysisId())
                || !Objects.equals(row.courseId(), sampling.courseId())
                || !Objects.equals(row.revNo(), sampling.revNo())
                || !Objects.equals(row.demId(), prepared.demId())
                || row.sampleIntervalM() == null
                || sampling.sampleIntervalM() == null
                || row.sampleIntervalM().compareTo(sampling.sampleIntervalM()) != 0) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }

        return row;
    }

    private List<ElevationSampleSaveRow> validateAndConvertSamples(
            PreparedTerrainAnalysis prepared, CourseElevationProfile profile) {
        CourseSamplingResult sampling = prepared.sampling();

        requireResult(
                profile != null
                        && profile.courseId() == sampling.courseId()
                        && profile.revNo() == sampling.revNo()
                        && profile.demId() == prepared.demId()
                        && profile.sampleIntervalM() != null
                        && profile.sampleIntervalM().compareTo(sampling.sampleIntervalM()) == 0
                        && Double.compare(profile.actualLengthM(), sampling.actualLengthM()) == 0);

        int count = profile.samples().size();

        requireResult(
                count >= 2
                        && count <= 2000
                        && count == sampling.samples().size()
                        && count == profile.totalSampleCount());

        requireResult(
                Double.isFinite(profile.minElevationM())
                        && Double.isFinite(profile.maxElevationM())
                        && profile.minElevationM() <= profile.maxElevationM()
                        && Double.isFinite(profile.ascentM())
                        && profile.ascentM() >= 0
                        && Double.isFinite(profile.descentM())
                        && profile.descentM() >= 0);

        List<ElevationSampleSaveRow> rows = new ArrayList<>(count);
        int validCount = 0;
        int validSegmentCount = 0;

        for (int index = 0; index < count; index++) {
            var sample = profile.samples().get(index);
            var source = sampling.samples().get(index);

            requireResult(
                    sample != null
                            && source != null
                            && sample.seqNo() == index + 1
                            && sample.seqNo() == source.seqNo()
                            && Double.isFinite(sample.distanceM())
                            && sample.distanceM() >= 0
                            && Double.isFinite(sample.longitude())
                            && sample.longitude() >= -180
                            && sample.longitude() <= 180
                            && Double.isFinite(sample.latitude())
                            && sample.latitude() >= -90
                            && sample.latitude() <= 90
                            && Double.compare(sample.distanceM(), source.distanceM()) == 0
                            && Double.compare(sample.longitude(), source.longitude()) == 0
                            && Double.compare(sample.latitude(), source.latitude()) == 0
                            && sample.status() != null);

            if (index == 0) {
                requireResult(sample.distanceM() == 0);
            } else {
                requireResult(sample.distanceM() > profile.samples().get(index - 1).distanceM());
            }

            BigDecimal elevationM = null;

            if (sample.status() == ElevationStatus.VALID) {
                requireResult(sample.elevationM() != null && Double.isFinite(sample.elevationM()));

                elevationM = decimal(sample.elevationM(), 7, 2);
                validCount++;

                if (index > 0
                        && profile.samples().get(index - 1).status() == ElevationStatus.VALID) {
                    validSegmentCount++;
                }
            } else {
                requireResult(sample.elevationM() == null);
            }

            rows.add(
                    new ElevationSampleSaveRow(
                            sample.seqNo(), decimal(sample.distanceM(), 9, 1), elevationM));
        }

        requireResult(
                Double.compare(
                                profile.samples().get(count - 1).distanceM(),
                                sampling.actualLengthM())
                        == 0);

        requireResult(
                validCount > 0
                        && profile.validSampleCount() == validCount
                        && profile.missingSampleCount() == count - validCount
                        && profile.validSegmentCount() == validSegmentCount);

        return List.copyOf(rows);
    }

    /** DB의 NUMERIC 정밀도와 범위를 검사한 뒤 저장 자릿수로 반올림한다. */
    private BigDecimal decimal(double value, int precision, int scale) {
        if (!Double.isFinite(value)) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        BigDecimal rounded = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);

        if (rounded.precision() > precision) {
            throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY);
        }

        return rounded;
    }

    private void requireResult(boolean condition) {
        if (!condition) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
