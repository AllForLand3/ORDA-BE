package com.allforland.orda.domain.analysis.terrain.controller;

import com.allforland.orda.common.error.ApiErrorResponse;
import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.logging.TraceIdFilter;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisRequest;
import com.allforland.orda.domain.analysis.terrain.dto.TerrainAnalysisResponse;
import com.allforland.orda.domain.analysis.terrain.service.TerrainAnalysisExecutionException;
import com.allforland.orda.domain.analysis.terrain.service.TerrainAnalysisQueryService;
import com.allforland.orda.domain.analysis.terrain.service.TerrainAnalysisService;
import java.net.URI;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class TerrainAnalysisController {

    private final TerrainAnalysisService analysisService;
    private final TerrainAnalysisQueryService queryService;

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

        TerrainAnalysisResponse response =
            queryService.findOwned(analysisId);

        HttpStatus status = responseStatus(response.getStatus());

        return ResponseEntity.status(status)
            .cacheControl(CacheControl.noStore())
            .location(analysisLocation(analysisId))
            .body(response);
    }

    /** 분석 ID를 확보한 실행 실패에 조회 경로와 공통 오류 본문을 제공한다. */
    @ExceptionHandler(TerrainAnalysisExecutionException.class)
    public ResponseEntity<ApiErrorResponse> handleExecutionFailure(
        TerrainAnalysisExecutionException exception,
        HttpServletRequest request) {

        ErrorCode code = exception.getErrorCode();

        ApiErrorResponse body =
            new ApiErrorResponse(
                code.getStatus().value(),
                code.name(),
                code.getMessage(),
                List.of(),
                TraceIdFilter.getOrCreateTraceId(request));

        return ResponseEntity.status(code.getStatus())
            .cacheControl(CacheControl.noStore())
            .location(analysisLocation(exception.getAnalysisId()))
            .body(body);
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
