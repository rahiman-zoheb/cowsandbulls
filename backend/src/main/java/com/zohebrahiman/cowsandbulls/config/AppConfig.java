package com.zohebrahiman.cowsandbulls.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({ GameLimits.class, ApiLimits.class })
public class AppConfig {

	/**
	 * Injected rather than calling Instant.now() directly, so expiry and rate
	 * limits can be tested without waiting for real time to pass.
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
