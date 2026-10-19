package com.zohebrahiman.cowsandbulls.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.zohebrahiman.cowsandbulls.support.MutableClock;

/**
 * The limits wired into the running application: the filter is registered,
 * reads its configuration, and answers before any controller runs (#12), and
 * actuator exposes nothing beyond health (#16).
 */
@SpringBootTest(properties = { "cowsandbulls.api.games-per-minute=3", "cowsandbulls.api.requests-per-minute=1000",
		"cowsandbulls.games.max-active=100" })
@AutoConfigureMockMvc
class RequestLimitsIntegrationTest {

	// Fixed time: with the real clock, a run that straddled a minute boundary
	// would reset the window between the 3rd and 4th request and fail at random.
	@TestConfiguration
	static class Clocks {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(Instant.parse("2026-10-18T12:00:10Z"));
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void theFourthNewGameInAMinuteIs429() throws Exception {
		// A client address no other test in this context uses, so the count starts at zero.
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/games").with(r -> { r.setRemoteAddr("192.0.2.1"); return r; }))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(post("/api/games").with(r -> { r.setRemoteAddr("192.0.2.1"); return r; }))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/problem+json")))
				.andExpect(jsonPath("$.detail").value("Too many new games; try again in a minute"));
	}

	@Test
	void anOversizedGuessIs413() throws Exception {
		String body = "{\"name\":\"" + "a".repeat(2000) + "\"}";

		mockMvc.perform(post("/api/games/{id}/guesses", "00000000-0000-4000-8000-000000000000")
						.with(r -> { r.setRemoteAddr("192.0.2.2"); return r; })
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isPayloadTooLarge());
	}

	@Test
	void actuatorExposesOnlyHealth() throws Exception {
		// application.properties used to set the Spring Boot 1.x property
		// endpoints.shutdown.enabled=true. It was inert, but "modernising" it
		// would have exposed an unauthenticated remote shutdown (#16).
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(post("/actuator/shutdown")).andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/configprops")).andExpect(status().isNotFound());
	}
}
