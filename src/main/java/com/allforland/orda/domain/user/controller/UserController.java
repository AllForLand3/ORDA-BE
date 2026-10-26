package com.allforland.orda.domain.user.controller;

import com.allforland.orda.domain.user.dto.SignupRequest;
import com.allforland.orda.domain.user.dto.SignupResponse;
import com.allforland.orda.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성합니다.")
    @SecurityRequirements
    @PostMapping("/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody SignupRequest request) {
        return users.signup(request);
    }
}
