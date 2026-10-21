package com.allforland.orda.domain.user.controller;

import com.allforland.orda.domain.user.dto.SignupRequest;
import com.allforland.orda.domain.user.dto.SignupResponse;
import com.allforland.orda.domain.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping("/api/users/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody SignupRequest request) {
        return users.signup(request);
    }
}
