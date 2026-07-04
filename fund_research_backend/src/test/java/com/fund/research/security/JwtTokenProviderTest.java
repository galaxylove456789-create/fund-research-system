package com.fund.research.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void generateAndParseTokenPreservesUserIdentityAndRole() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-with-enough-length-for-hmac-sha");
        properties.setExpireMinutes(60L);

        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateToken(7L, "alice", "ADMIN");
        AuthenticatedUser user = provider.parseToken(token);

        assertThat(user.getUserId()).isEqualTo(7L);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getRoleCode()).isEqualTo("ADMIN");
        assertThat(user.isAdmin()).isTrue();
    }
}
