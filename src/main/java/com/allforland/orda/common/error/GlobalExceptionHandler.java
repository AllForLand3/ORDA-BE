package com.allforland.orda.common.error;

import com.allforland.orda.common.logging.TraceIdFilter;
import java.net.URI;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        if (status.is5xxServerError()) {
            LOG.error(
                    "Request failed: traceId={}, type={}",
                    TraceIdFilter.getOrCreateTraceId(servletRequest),
                    exception.getClass().getSimpleName());
        }
        return new ResponseEntity<>(
                response(status, ErrorCode.fromStatus(status), details(exception), servletRequest),
                headers,
                status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof BusinessException business
                    && business.getErrorCode() == ErrorCode.INVALID_COURSE_GEOMETRY) {
                HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
                return new ResponseEntity<>(
                        response(status, business.getErrorCode(), List.of(), servletRequest),
                        headers,
                        status);
            }
            cause = cause.getCause();
        }
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    /** 분석 실행 실패도 공통 오류 형식으로 반환하고 결과 조회 경로를 제공한다. */
    @ExceptionHandler(TerrainAnalysisExecutionException.class)
    public ResponseEntity<ApiErrorResponse> handleAnalysisExecution(
            TerrainAnalysisExecutionException exception, HttpServletRequest request) {
        ErrorCode code = exception.getErrorCode();
        URI location =
                ServletUriComponentsBuilder.fromContextPath(request)
                        .path("/api/terrain-analyses/{analysisId}")
                        .buildAndExpand(exception.getAnalysisId())
                        .toUri();
        if (code.getStatus().is5xxServerError()) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            LOG.error(
                    "Request failed: traceId={}, analysisId={}, type={}",
                    TraceIdFilter.getOrCreateTraceId(request),
                    exception.getAnalysisId(),
                    cause.getClass().getSimpleName());
        }
        return ResponseEntity.status(code.getStatus())
                .cacheControl(CacheControl.noStore())
                .location(location)
                .body(response(code.getStatus(), code, List.of(), request));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(
            BusinessException exception, HttpServletRequest request) {
        ErrorCode code = exception.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(response(code.getStatus(), code, List.of(), request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        ErrorCode code = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.badRequest()
                .body(response(code.getStatus(), code, List.of(), request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getRawStatusCode();
        if (status >= 500) {
            LOG.error(
                    "Request failed: traceId={}, type={}",
                    TraceIdFilter.getOrCreateTraceId(request),
                    exception.getClass().getSimpleName());
        }
        return ResponseEntity.status(status)
                .headers(exception.getResponseHeaders())
                .body(response(status, ErrorCode.fromStatus(status), List.of(), request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        ResponseStatus responseStatus =
                AnnotatedElementUtils.findMergedAnnotation(
                        exception.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            HttpStatus status = responseStatus.code();
            if (status.is5xxServerError()) {
                LOG.error(
                        "Request failed: traceId={}, type={}",
                        TraceIdFilter.getOrCreateTraceId(request),
                        exception.getClass().getSimpleName());
            }
            return ResponseEntity.status(status)
                    .body(response(status, ErrorCode.fromStatus(status), List.of(), request));
        }
        LOG.error(
                "Request failed: traceId={}, type={}",
                TraceIdFilter.getOrCreateTraceId(request),
                exception.getClass().getSimpleName());
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(response(code.getStatus(), code, List.of(), request));
    }

    private ApiErrorResponse response(
            HttpStatus status,
            ErrorCode code,
            List<ApiErrorResponse.FieldViolation> details,
            HttpServletRequest request) {
        return response(status.value(), code, details, request);
    }

    private ApiErrorResponse response(
            int status,
            ErrorCode code,
            List<ApiErrorResponse.FieldViolation> details,
            HttpServletRequest request) {
        return new ApiErrorResponse(
                status,
                code.name(),
                code.getMessage(),
                details,
                TraceIdFilter.getOrCreateTraceId(request));
    }

    private List<ApiErrorResponse.FieldViolation> details(Exception exception) {
        BindingResult bindingResult;
        if (exception instanceof MethodArgumentNotValidException invalid) {
            bindingResult = invalid.getBindingResult();
        } else if (exception instanceof BindException invalid) {
            bindingResult = invalid.getBindingResult();
        } else {
            return List.of();
        }
        return bindingResult.getFieldErrors().stream()
                .map(error -> new ApiErrorResponse.FieldViolation(error.getField(), "입력값을 확인해주세요."))
                .toList();
    }
}
