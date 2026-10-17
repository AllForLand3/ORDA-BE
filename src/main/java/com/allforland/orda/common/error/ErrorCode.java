package com.allforland.orda.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이나 입력값을 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다."),
    CONFLICT(HttpStatus.CONFLICT, "현재 상태와 요청이 충돌합니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "요청을 처리할 수 없는 상태입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "요청 본문이 허용 크기를 초과했습니다."),
    PRECONDITION_FAILED(HttpStatus.PRECONDITION_FAILED, "요청 조건이 일치하지 않습니다."),
    PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "요청 조건이 필요합니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 사용할 수 없습니다."),
    INVALID_COURSE_GEOMETRY(HttpStatus.BAD_REQUEST, "코스 형상과 좌표를 확인해주세요."),
    COURSE_OUTSIDE_SERVICE_AREA(HttpStatus.BAD_REQUEST, "코스 전체가 해당 산의 서비스 영역 안에 있어야 합니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
    MOUNTAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "사용 가능한 산을 찾을 수 없습니다."),
    COURSE_EDIT_CONFLICT(HttpStatus.PRECONDITION_FAILED, "코스가 변경되었습니다. 최신 내용을 다시 조회해주세요."),
    COURSE_PRECONDITION_REQUIRED(HttpStatus.PRECONDITION_REQUIRED, "If-Match 헤더가 필요합니다."),
    COURSE_AREA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "유효한 활성 서비스 영역을 확인할 수 없습니다."),
    COURSE_WORKSPACE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "시연 작업공간 계정 설정을 확인해주세요."),
    COURSE_STATE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "코스 저장 상태를 확인해야 합니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorCode fromStatus(HttpStatus status) {
        return fromStatus(status.value());
    }

    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> INVALID_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> CONFLICT;
            case 412 -> PRECONDITION_FAILED;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 422 -> UNPROCESSABLE_ENTITY;
            case 428 -> PRECONDITION_REQUIRED;
            case 429 -> TOO_MANY_REQUESTS;
            case 503 -> SERVICE_UNAVAILABLE;
            default -> status >= 400 && status < 500 ? INVALID_REQUEST : INTERNAL_SERVER_ERROR;
        };
    }
}
