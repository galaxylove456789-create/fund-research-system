package com.fund.research.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;

    private Long expireMinutes = 1440L;

    private String header = "Authorization";

    private String prefix = "Bearer ";
}
