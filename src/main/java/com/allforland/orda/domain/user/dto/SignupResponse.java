package com.allforland.orda.domain.user.dto;

/** bigint 식별자는 코스 API와 동일하게 문자열로 반환한다. */
public record SignupResponse(String userId, String loginId, String nickname) {}
