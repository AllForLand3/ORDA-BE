package com.allforland.orda.domain.user.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;

/** 회원 입력을 정규화하고 DB와 BCrypt 길이 제한을 검증한다. */
public final class AccountInput {
    private AccountInput() {}

    public static String loginId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,49}")) {
            throw invalid();
        }
        return value;
    }

    public static String loginIdForLogin(String value) {
        if (value == null) {
            throw invalid();
        }

        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._-]{2,49}")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return value;
    }

    public static String userName(String value) {
        text(value);
        String name = value.strip();
        if (name.isBlank()
                || name.codePointCount(0, name.length()) > 50
                || !name.matches("[\\p{L}\\p{M}\\p{N} _.-]+")) {
            throw invalid();
        }
        return name;
    }

    public static String password(String value) {
        return password(value, false);
    }

    public static String loginPassword(String value) {
        return password(value, true);
    }

    private static String password(String value, boolean login) {
        text(value);
        if (value.isBlank()) {
            throw invalid();
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > 72) {
            if (login) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            throw invalid();
        }
        return value;
    }

    private static void text(String value) {
        if (value == null) {
            throw invalid();
        }
        for (int index = 0; index < value.length(); ) {
            int point = value.codePointAt(index);
            if (Character.isISOControl(point)
                    || (point >= 0xD800 && point <= 0xDFFF)
                    || (point >= 0xFDD0 && point <= 0xFDEF)
                    || (point & 0xFFFE) == 0xFFFE) {
                throw invalid();
            }
            index += Character.charCount(point);
        }
    }

    private static BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
