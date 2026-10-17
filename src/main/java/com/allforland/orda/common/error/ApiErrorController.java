package com.allforland.orda.common.error;

import com.allforland.orda.common.logging.TraceIdFilter;
import java.util.List;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ApiErrorResponse> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = value instanceof Integer number ? HttpStatus.resolve(number) : null;
        if (status == null || !status.isError()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        ErrorCode code = ErrorCode.fromStatus(status);
        String traceId = TraceIdFilter.getOrCreateTraceId(request);
        return ResponseEntity.status(status)
                .header(TraceIdFilter.HEADER_NAME, traceId)
                .body(
                        new ApiErrorResponse(
                                status.value(),
                                code.name(),
                                code.getMessage(),
                                List.of(),
                                traceId));
    }
}
