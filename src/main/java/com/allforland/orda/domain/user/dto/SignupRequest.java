package com.allforland.orda.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

/** 외부 nickname은 Service에서 user_nm에 매핑한다. 비밀번호를 문자열 표현에 넣지 않는다. */
@Getter
@Setter
public class SignupRequest {
    private String loginId;
    private String nickname;
    private String password;
}
