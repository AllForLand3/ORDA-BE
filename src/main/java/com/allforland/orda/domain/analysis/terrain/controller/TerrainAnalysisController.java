package com.allforland.orda.domain.analysis.terrain.controller;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisRequest;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse;
import com.allforland.orda.domain.analysis.terrain.service.TerrainAnalysisQueryService;
import com.allforland.orda.domain.analysis.terrain.service.TerrainAnalysisService;
import java.math.BigDecimal;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class TerrainAnalysisController {

    private final TerrainAnalysisService analysisService;
    private final TerrainAnalysisQueryService queryService;

    /** 현재 코스 버전의 지정 조건 분석 요약을 조회한다. */
    @GetMapping("/courses/{courseId}/terrain")
    public ResponseEntity<TerrainAnalysisResponse> getCurrentSummary(
            @PathVariable("courseId") String courseId,
            @RequestParam("demId") String demId,
            @RequestParam(value = "sampleIntervalM", required = false) BigDecimal sampleIntervalM,
            @RequestParam(value = "revNo", required = false) Integer revNo) {

        TerrainAnalysisResponse response =
                queryService.findCurrentSummary(
                        positiveId(courseId), positiveId(demId), sampleIntervalM, revNo);

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 현재 코스 버전의 지정 조건 분석 요약과 전체 샘플을 조회한다. */
    @GetMapping("/courses/{courseId}/elevation-profile")
    public ResponseEntity<TerrainAnalysisResponse> getCurrentProfile(
            @PathVariable("courseId") String courseId,
            @RequestParam("demId") String demId,
            @RequestParam(value = "sampleIntervalM", required = false) BigDecimal sampleIntervalM,
            @RequestParam(value = "revNo", required = false) Integer revNo) {

        TerrainAnalysisResponse response =
                queryService.findCurrentProfile(
                        positiveId(courseId), positiveId(demId), sampleIntervalM, revNo);

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 분석 ID로 상태·요약·전체 샘플을 조회한다. */
    @GetMapping("/terrain-analyses/{analysisId}")
    public ResponseEntity<TerrainAnalysisResponse> getAnalysis(
            @PathVariable("analysisId") String analysisId) {

        TerrainAnalysisResponse response = queryService.findOwned(positiveId(analysisId));

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    /** 분석을 동기 실행하거나 동일 조건의 기존 결과를 반환한다. */
    @PostMapping("/courses/{courseId}/terrain-analyses")
    public ResponseEntity<TerrainAnalysisResponse> analyze(
            @PathVariable("courseId") String courseId,
            @RequestBody TerrainAnalysisRequest request) {

        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        long parsedCourseId = positiveId(courseId);
        long parsedDemId = positiveId(request.getDemId());

        long analysisId =
                analysisService.analyze(
                        parsedCourseId,
                        request.getRevNo(),
                        parsedDemId,
                        request.getSampleIntervalM());

        TerrainAnalysisResponse response = queryService.findOwned(analysisId);

        HttpStatus status = responseStatus(response.getStatus());

        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .location(analysisLocation(analysisId))
                .body(response);
    }

    private HttpStatus responseStatus(String status) {
        if (status == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return switch (status) {
            case "REQUESTED", "RUNNING" -> HttpStatus.ACCEPTED;
            case "DONE", "FAILED" -> HttpStatus.OK;
            default -> throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        };
    }

    private URI analysisLocation(long analysisId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/terrain-analyses/{analysisId}")
                .buildAndExpand(analysisId)
                .toUri();
    }

    private long positiveId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
