package com.fund.research.security;

import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validate(String username, String password) {
        if (!StringUtils.hasText(password) || password.length() < 8 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码长度必须为 8-32 位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "密码必须同时包含字母和数字");
        }
        if (StringUtils.hasText(username)) {
            String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
            String normalizedPassword = password.trim().toLowerCase(Locale.ROOT);
            if (normalizedPassword.equals(normalizedUsername) || normalizedPassword.contains(normalizedUsername)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "密码不能包含用户名");
            }
        }
    }
}
