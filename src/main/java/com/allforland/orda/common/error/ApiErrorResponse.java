package com.allforland.orda.common.error;

import java.util.List;

public record ApiErrorResponse(
        int status, String code, String message, List<FieldViolation> details, String traceId) {
    public record FieldViolation(String field, String message) {}
}
