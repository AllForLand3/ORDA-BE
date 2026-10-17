package com.allforland.orda.common.error;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"status", "code", "message", "details", "traceId"})
public class ApiErrorResponse {
    private final int status;
    private final String code;
    private final String message;
    private final List<FieldViolation> details;
    private final String traceId;

    @Getter
    @Accessors(fluent = true)
    @AllArgsConstructor
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE,
            setterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonPropertyOrder({"field", "message"})
    public static class FieldViolation {
        private final String field;
        private final String message;
    }
}
