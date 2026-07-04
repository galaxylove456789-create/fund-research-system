package com.fund.research.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify workflow integration properties.
 * <p>
 * API key must never be exposed to frontend; only server reads it from env/config.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    /** Dify API base URL, e.g. https://api.dify.ai/v1 */
    private String baseUrl;

    /** Dify API key. Loaded from env var DIFY_API_KEY in production. */
    private String apiKey;

    /** Default request timeout in milliseconds. */
    private Integer timeoutMillis = 30000;
}
