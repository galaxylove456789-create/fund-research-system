package com.fund.research.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final Key signingKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        String secret = StringUtils.hasText(properties.getSecret())
                ? properties.getSecret()
                : "please-change-me-in-prod-with-at-least-32-bytes";
        this.signingKey = Keys.hmacShaKeyFor(padSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String roleCode) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpireMinutes() * 60);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roleCode", StringUtils.hasText(roleCode) ? roleCode : "USER")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String roleCode = claims.get("roleCode", String.class);
        return new AuthenticatedUser(userId, username, roleCode);
    }

    private String padSecret(String secret) {
        if (secret.length() >= 32) {
            return secret;
        }
        return (secret + "00000000000000000000000000000000").substring(0, 32);
    }
}
