package com.allforland.orda.domain.auth.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.common.security.JwtTokenService;
import com.allforland.orda.domain.auth.dto.AuthenticatedUser;
import com.allforland.orda.domain.auth.dto.CurrentUserResponse;
import com.allforland.orda.domain.auth.dto.LoginRequest;
import com.allforland.orda.domain.auth.dto.LoginResponse;
import com.allforland.orda.domain.user.mapper.UserMapper;
import com.allforland.orda.domain.user.service.AccountInput;
import com.allforland.orda.domain.user.vo.UserAccount;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper users;
    private final BCryptPasswordEncoder passwords;
    private final JwtTokenService tokens;
    private final String dummyHash;

    public AuthService(UserMapper users, BCryptPasswordEncoder passwords, JwtTokenService tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        String loginId = AccountInput.loginIdForLogin(request.getLoginId());
        String password = AccountInput.loginPassword(request.getPassword());
        UserAccount user = users.findByLoginId(loginId);
        boolean eligible = user != null && user.authenticatable();
        boolean matches = passwords.matches(password, eligible ? user.passwordHash() : dummyHash);
        if (!eligible || !matches) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return new LoginResponse(
                tokens.issue(user.userId()),
                "Bearer",
                Long.toString(user.userId()),
                user.loginId(),
                user.userNm(),
                user.roleCd());
    }

    public CurrentUserResponse me() {
        AuthenticatedUser user = requireCurrentUser();
        return new CurrentUserResponse(
                Long.toString(user.userId()), user.loginId(), user.userNm(), user.roleCd());
    }

    /** 토큰의 역할 대신 요청 시점의 DB 회원 상태를 사용한다. */
    public AbstractAuthenticationToken authenticate(Jwt token) {
        long userId;
        try {
            String subject = token.getSubject();
            if (subject == null || !subject.matches("[1-9][0-9]{0,18}")) {
                throw new NumberFormatException();
            }
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new BadCredentialsException("Invalid credentials");
        }
        UserAccount account;
        try {
            account = users.findByUserId(userId);
        } catch (DataAccessException exception) {
            throw new AuthenticationServiceException("Authentication storage unavailable");
        }
        if (account == null || !account.authenticatable()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new UsernamePasswordAuthenticationToken(
                AuthenticatedUser.from(account),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.roleCd())));
    }

    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
