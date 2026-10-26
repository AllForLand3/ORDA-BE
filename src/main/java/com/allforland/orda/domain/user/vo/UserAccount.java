package com.allforland.orda.domain.user.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** 인증에만 사용하는 DB 행이며 API 응답으로 반환하지 않는다. */
@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class UserAccount {
    private final Long userId;
    private final String loginId;
    private final String userNm;
    private final String roleCd;
    private final String useYn;
    @JsonIgnore private final String passwordHash;

    public boolean authenticatable() {
        return userId != null
                && userId > 0
                && "Y".equals(useYn)
                && ("USER".equals(roleCd) || "ADMIN".equals(roleCd))
                && passwordHash != null
                && passwordHash.matches(
                        "\\$2[aby]\\$(?:0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");
    }
}
