package com.fund.research.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenProviderTest {

    @Test
    void generateTokenCanBeParsedBackToAuthenticatedUser() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setExpireMinutes(30L);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateToken(1001L, "fundUser", "ADMIN");
        AuthenticatedUser user = provider.parseToken(token);

        assertNotNull(token);
        assertEquals(1001L, user.getUserId());
        assertEquals("fundUser", user.getUsername());
        assertEquals("ADMIN", user.getRoleCode());
    }

    @Test
    void generateTokenUsesUserRoleWhenRoleCodeIsBlank() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setExpireMinutes(30L);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateToken(1002L, "normalUser", "");
        AuthenticatedUser user = provider.parseToken(token);

        assertEquals("USER", user.getRoleCode());
    }
}
