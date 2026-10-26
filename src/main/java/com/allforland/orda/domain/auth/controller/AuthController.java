package com.allforland.orda.domain.auth.controller;

import com.allforland.orda.domain.auth.dto.CurrentUserResponse;
import com.allforland.orda.domain.auth.dto.LoginRequest;
import com.allforland.orda.domain.auth.dto.LoginResponse;
import com.allforland.orda.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @Operation(summary = "로그인", description = "로그인 ID와 비밀번호로 JWT access token을 발급합니다.")
    @SecurityRequirements
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.login(request));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(auth.me());
    }
}
