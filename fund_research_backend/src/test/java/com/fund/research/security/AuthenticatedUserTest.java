package com.fund.research.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedUserTest {

    @Test
    void defaultsNullRoleToUser() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "guest", null);

        assertEquals("USER", user.getRoleCode());
        assertEquals("guest", user.getUsername());
        assertEquals(1L, user.getUserId());
    }

    @Test
    void convertsRoleToAuthorityName() {
        AuthenticatedUser user = new AuthenticatedUser(2L, "admin", "admin");

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertTrue(user.isAdmin());
        assertEquals("ROLE_ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void accountFlagsAreEnabledForAuthenticatedUser() {
        AuthenticatedUser user = new AuthenticatedUser(3L, "user", "USER");

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }
}
