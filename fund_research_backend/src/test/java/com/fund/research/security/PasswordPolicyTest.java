package com.fund.research.security;

import com.fund.research.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void rejectsWeakPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.validate("alice", "12345678"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字母和数字");

        assertThatThrownBy(() -> PasswordPolicy.validate("alice", "alice2026"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名");
    }

    @Test
    void acceptsPasswordWithLettersAndNumbers() {
        assertThatCode(() -> PasswordPolicy.validate("alice", "Fund2026"))
                .doesNotThrowAnyException();
    }
}
