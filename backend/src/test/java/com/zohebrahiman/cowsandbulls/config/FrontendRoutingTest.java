package com.zohebrahiman.cowsandbulls.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #46: the backend serves the frontend build. src/test/resources/static holds
 * a stand-in index.html and a hashed asset in place of the real Vite output.
 */
@SpringBootTest(properties = "cowsandbulls.api.rate-limits-enabled=false")
@AutoConfigureMockMvc
class FrontendRoutingTest {

	private static final String SPA = "SPA FIXTURE";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void theRootForwardsToTheApp() throws Exception {
		// Spring Boot's welcome page answers "/" by forwarding to index.html.
		// MockMvc records forwards rather than following them; the container
		// smoke test in CI checks the real response.
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(SPA)));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/games/new", "/games/3f1c2a6e-8a4b-4d2c-9e7f-1a2b3c4d5e6f", "/guesses", "/some/unknown/page" })
	void clientSideRoutesAreAnsweredWithTheApp(String path) throws Exception {
		// These only exist in the React router; a refresh or shared link has to
		// get index.html, or it would be a server 404.
		mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/html")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(SPA)));
	}

	@Test
	void hashedAssetsAreServedAndCachedForAYear() throws Exception {
		mockMvc.perform(get("/assets/app-1a2b3c.js"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("javascript")))
				.andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
	}

	@Test
	void theAppShellIsRevalidatedOnEveryLoadSoDeploysShowUp() throws Exception {
		mockMvc.perform(get("/games/new")).andExpect(header().string("Cache-Control", "no-cache"));
	}

	@Test
	void aMissingAssetIs404NotTheApp() throws Exception {
		// Serving HTML for a missing script would turn a broken deploy into a
		// confusing "Unexpected token <" in the browser.
		mockMvc.perform(get("/assets/gone-9z9z9z.js")).andExpect(status().isNotFound());
		mockMvc.perform(get("/missing.png")).andExpect(status().isNotFound());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/nope", "/api", "/actuator/env" })
	void unknownApiAndActuatorPathsAre404NotTheApp(String path) throws Exception {
		// A typo in an API path must fail loudly, not return a 200 HTML page.
		String body = mockMvc.perform(get(path))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain(SPA);
	}

	@Test
	void theApiStillWorksAlongsideTheApp() throws Exception {
		mockMvc.perform(post("/api/games")).andExpect(status().isCreated());
		mockMvc.perform(get("/api/games/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/", "/index.html", "/games/new", "/api/games/00000000-0000-4000-8000-000000000000" })
	void everyResponseCarriesTheSecurityHeaders(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(header().string("Content-Security-Policy", SecurityHeadersFilter.CONTENT_SECURITY_POLICY))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"));
	}

	@Test
	void thePolicyForbidsFramingAndForeignScripts() {
		assertThat(SecurityHeadersFilter.CONTENT_SECURITY_POLICY)
				.contains("default-src 'self'", "frame-ancestors 'none'", "object-src 'none'")
				.doesNotContain("unsafe-inline", "unsafe-eval", "*");
	}

	@Test
	void clientRouteDetectionExcludesApiActuatorAndFiles() {
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("games/new")).isTrue();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("games/3f1c2a6e")).isTrue();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("api/games")).isFalse();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("api")).isFalse();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("actuator/health")).isFalse();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("assets/app.js")).isFalse();
		assertThat(FrontendConfig.ClientRouteResolver.isClientRoute("apiary")).as("only the api/ segment, not a prefix").isTrue();
	}
}
