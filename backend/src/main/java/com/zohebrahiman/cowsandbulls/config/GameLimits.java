package com.zohebrahiman.cowsandbulls.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How long idle games are kept, and how many may exist at once.
 */
@ConfigurationProperties("cowsandbulls.games")
public record GameLimits(Duration ttl, Duration cleanupInterval, long maxActive) {
}
