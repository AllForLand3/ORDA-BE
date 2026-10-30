package com.allforland.orda.domain.analysis.mapper;

import com.allforland.orda.domain.analysis.vo.ElevationSampleSaveRow;
import com.allforland.orda.domain.analysis.vo.TerrainAnalysisRow;
import com.allforland.orda.domain.analysis.vo.TerrainAnalysisSummarySaveRow;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TerrainAnalysisMapper {

    /** 신규 요청을 등록하고 ID를 반환한다. 동일 조건이 이미 있으면 null을 반환한다. */
    Long insertRequested(
            @Param("courseId") long courseId,
            @Param("revNo") int revNo,
            @Param("demId") long demId,
            @Param("sampleIntervalM") BigDecimal sampleIntervalM);

    /** 준비 트랜잭션 동안 소유자 코스의 변경·삭제를 막는다. */
    Long lockOwnedCourse(@Param("courseId") long courseId, @Param("ownerUserId") long ownerUserId);

    /** 동일한 분석 조건의 기존 행을 조회한다. */
    TerrainAnalysisRow findByCondition(
            @Param("courseId") long courseId,
            @Param("revNo") int revNo,
            @Param("demId") long demId,
            @Param("sampleIntervalM") BigDecimal sampleIntervalM);

    /** 상태 변경을 위해 분석 행을 잠근다. 쓰기 트랜잭션 안에서 호출한다. */
    TerrainAnalysisRow findByIdForUpdate(@Param("analysisId") long analysisId);

    /** 요청 상태인 분석의 실행 권한을 확보한다. 변경한 행 수를 반환한다. */
    int markRunning(@Param("analysisId") long analysisId);

    /** 분석 샘플을 일괄 저장하고 저장한 행 수를 반환한다. */
    int insertSamples(
            @Param("analysisId") long analysisId,
            @Param("samples") List<ElevationSampleSaveRow> samples);

    /** 실행 중인 분석의 요약과 정상 완료 상태를 함께 저장한다. */
    int markDone(
            @Param("analysisId") long analysisId,
            @Param("summary") TerrainAnalysisSummarySaveRow summary);

    /** 실행 중인 분석에 안전한 오류 안내와 실패 상태를 기록한다. */
    int markFailed(
            @Param("analysisId") long analysisId, @Param("errorMessage") String errorMessage);
}
