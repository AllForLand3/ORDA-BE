package com.allforland.orda.domain.auth.dto;

public record CurrentUserResponse(String userId, String loginId, String nickname, String role) {}
