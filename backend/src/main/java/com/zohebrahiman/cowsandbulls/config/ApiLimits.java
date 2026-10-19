package com.zohebrahiman.cowsandbulls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-client request limits for /api, and the largest request body accepted.
 */
@ConfigurationProperties("cowsandbulls.api")
public record ApiLimits(boolean rateLimitsEnabled, int requestsPerMinute, int gamesPerMinute, long maxRequestBytes) {
}
