package com.allforland.orda.domain.analysis.terrain.service;

import com.allforland.orda.domain.analysis.terrain.vo.CourseElevationProfile;
import com.allforland.orda.domain.analysis.terrain.vo.PreparedTerrainAnalysis;
import com.allforland.orda.domain.analysis.terrain.vo.TerrainAnalysisPreparation;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 분석 준비·고도 계산·결과 저장의 전체 흐름을 조합한다. */
@Service
@RequiredArgsConstructor
public class TerrainAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(TerrainAnalysisService.class);

    private final TerrainAnalysisTransactionService transactionService;
    private final CourseElevationProfileService profileService;

    /** 신규 분석을 동기 실행하거나 기존 분석 ID를 반환한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public long analyze(
            long courseId, int expectedRevNo, long demId, BigDecimal requestedIntervalM) {
        TerrainAnalysisPreparation preparation =
                transactionService.prepareAndStart(
                        courseId, expectedRevNo, demId, requestedIntervalM);

        if (!preparation.shouldExecute()) {
            return preparation.analysisId();
        }

        PreparedTerrainAnalysis prepared = preparation.prepared();

        try {
            CourseElevationProfile profile =
                    profileService.calculate(prepared.sampling(), prepared.demId());

            transactionService.complete(prepared, profile);

            return prepared.analysisId();
        } catch (RuntimeException failure) {
            recordFailure(prepared, failure);

            throw new TerrainAnalysisExecutionException(prepared.analysisId(), failure);
        }
    }

    /** 실패 기록 오류가 원래 계산·저장 오류를 덮어쓰지 않게 처리한다. */
    private void recordFailure(PreparedTerrainAnalysis prepared, RuntimeException originalFailure) {
        try {
            boolean recorded = transactionService.fail(prepared, originalFailure);

            if (!recorded) {
                LOG.warn(
                        "분석이 실행 중 상태가 아니어서 실패 상태를 변경하지 않았습니다. analysisId={}",
                        prepared.analysisId());
            }
        } catch (RuntimeException recordingFailure) {
            if (recordingFailure != originalFailure) {
                originalFailure.addSuppressed(recordingFailure);
            }

            LOG.error("분석 실패 상태를 기록하지 못했습니다. analysisId={}", prepared.analysisId());
        }
    }
}
