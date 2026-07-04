package com.fund.research.security;

import com.fund.research.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsPasswordWithLettersAndDigits() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("alice", "Fund2026"));
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("alice", "A12345"));
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("alice", "PasswordOnly"));
    }

    @Test
    void rejectsPasswordContainingUsernameIgnoringCase() {
        assertThrows(BusinessException.class, () -> PasswordPolicy.validate("Alice", "Alice2026"));
    }
}
