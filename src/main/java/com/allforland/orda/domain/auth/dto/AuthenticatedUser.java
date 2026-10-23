package com.allforland.orda.domain.auth.dto;

import com.allforland.orda.domain.user.vo.UserAccount;
import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** DB에서 확인한 현재 사용자 정보만 보관한다. 토큰과 비밀번호 해시는 보관하지 않는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class AuthenticatedUser implements Principal {
    private final long userId;
    private final String loginId;
    private final String userNm;
    private final String roleCd;

    public static AuthenticatedUser from(UserAccount account) {
        return new AuthenticatedUser(
                account.userId(), account.loginId(), account.userNm(), account.roleCd());
    }

    @Override
    public String getName() {
        return Long.toString(userId);
    }
}
