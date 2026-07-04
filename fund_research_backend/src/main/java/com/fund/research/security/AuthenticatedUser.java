package com.fund.research.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String roleCode;

    public AuthenticatedUser(Long userId, String username, String roleCode) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode == null ? "USER" : roleCode.toUpperCase(Locale.ROOT);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleCode);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
