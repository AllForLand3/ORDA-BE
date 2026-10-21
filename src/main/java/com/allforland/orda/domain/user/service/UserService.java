package com.allforland.orda.domain.user.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.user.dto.SignupRequest;
import com.allforland.orda.domain.user.dto.SignupResponse;
import com.allforland.orda.domain.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserMapper mapper;
    private final BCryptPasswordEncoder passwords;

    public UserService(UserMapper mapper, BCryptPasswordEncoder passwords) {
        this.mapper = mapper;
        this.passwords = passwords;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String loginId = AccountInput.loginId(request.getLoginId());
        String userNm = AccountInput.userName(request.getNickname());
        String password = AccountInput.password(request.getPassword());
        // PostgreSQL's targeted UNIQUE conflict handling also covers concurrent signups.
        Long userId = mapper.insertUser(loginId, userNm, passwords.encode(password));
        if (userId == null) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        return new SignupResponse(Long.toString(userId), loginId, userNm);
    }
}
