package com.allforland.orda.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 토큰을 toString에 포함하지 않는 로그인 응답이다. */
@Getter
@AllArgsConstructor
public class LoginResponse {
    private final String accessToken;
    private final String tokenType;
    private final String userId;
    private final String loginId;
    private final String nickname;
    private final String role;
}
