package com.zohebrahiman.cowsandbulls.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.zohebrahiman.cowsandbulls.support.MutableClock;

class ApiRequestLimitFilterTest {

	// 00:00:10 into a minute, so there's room to move within the window.
	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-18T12:00:10Z"));

	private ApiRequestLimitFilter filter;

	@BeforeEach
	void setUp() {
		filter = new ApiRequestLimitFilter(new ApiLimits(true, 5, 2, 1024), clock);
	}

	private MockHttpServletResponse send(String method, String uri, String client) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		request.setRemoteAddr(client);
		return send(request);
	}

	private MockHttpServletResponse send(MockHttpServletRequest request) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request, response, chain);
		if (chain.getRequest() != null) {
			response.setStatus(200); // reached the controller
		}
		return response;
	}

	@Test
	void requestsWithinTheLimitPassThrough() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(send("GET", "/api/games/x", "10.0.0.1").getStatus()).isEqualTo(200);
		}
	}

	@Test
	void theRequestAfterTheLimitIs429WithRetryAfterAndAProblemBody() throws Exception {
		for (int i = 0; i < 5; i++) {
			send("GET", "/api/games/x", "10.0.0.1");
		}

		MockHttpServletResponse response = send("GET", "/api/games/x", "10.0.0.1");

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("50");
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(response.getContentAsString())
				.isEqualTo("{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Too many requests; try again in a minute\"}");
	}

	@Test
	void creatingGamesHasItsOwnStricterLimit() throws Exception {
		assertThat(send("POST", "/api/games", "10.0.0.1").getStatus()).isEqualTo(200);
		assertThat(send("POST", "/api/games", "10.0.0.1").getStatus()).isEqualTo(200);

		MockHttpServletResponse third = send("POST", "/api/games", "10.0.0.1");
		assertThat(third.getStatus()).isEqualTo(429);
		assertThat(third.getContentAsString()).contains("Too many new games");

		// Playing the games already created is still allowed.
		assertThat(send("POST", "/api/games/x/guesses", "10.0.0.1").getStatus()).isEqualTo(200);
	}

	@Test
	void eachClientHasItsOwnBudget() throws Exception {
		for (int i = 0; i < 5; i++) {
			send("GET", "/api/games/x", "10.0.0.1");
		}

		assertThat(send("GET", "/api/games/x", "10.0.0.1").getStatus()).isEqualTo(429);
		assertThat(send("GET", "/api/games/x", "10.0.0.2").getStatus()).isEqualTo(200);
	}

	@Test
	void theBudgetResetsInTheNextMinute() throws Exception {
		for (int i = 0; i < 6; i++) {
			send("GET", "/api/games/x", "10.0.0.1");
		}

		clock.advance(Duration.ofSeconds(50));

		assertThat(send("GET", "/api/games/x", "10.0.0.1").getStatus()).isEqualTo(200);
	}

	@Test
	void countersFromFinishedMinutesAreEvicted() throws Exception {
		send("GET", "/api/games/x", "10.0.0.1");
		send("POST", "/api/games", "10.0.0.2");
		assertThat(filter.trackedClients()).isEqualTo(3);

		clock.advance(Duration.ofMinutes(1));
		filter.evictExpiredWindows();

		assertThat(filter.trackedClients()).isZero();
	}

	@Test
	void anOversizedBodyIs413BeforeAnyCounting() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/games/x/guesses");
		request.setContent(new byte[1025]);

		MockHttpServletResponse response = send(request);

		assertThat(response.getStatus()).isEqualTo(413);
		assertThat(response.getContentAsString()).contains("larger than 1024 bytes");
	}

	@Test
	void aBodyAtTheLimitIsAccepted() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/games/x/guesses");
		request.setContent(new byte[1024]);

		assertThat(send(request).getStatus()).isEqualTo(200);
	}

	@Test
	void aChunkedBodyOfUnknownSizeIs411() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/games/x/guesses");
		request.addHeader("Transfer-Encoding", "chunked");

		assertThat(send(request).getStatus()).isEqualTo(411);
	}

	@Test
	void aBodylessPostWithNoLengthHeaderIsFine() throws Exception {
		// curl -X POST /api/games sends neither Content-Length nor
		// Transfer-Encoding, and the CI smoke test relies on it.
		assertThat(send("POST", "/api/games", "10.0.0.1").getStatus()).isEqualTo(200);
	}

	@Test
	void pathsOutsideTheApiAreNotLimited() throws Exception {
		for (int i = 0; i < 20; i++) {
			assertThat(send("GET", "/actuator/health", "10.0.0.1").getStatus()).isEqualTo(200);
		}
	}

	@Test
	void rateLimitsCanBeSwitchedOffWithoutLosingTheSizeLimit() throws Exception {
		filter = new ApiRequestLimitFilter(new ApiLimits(false, 1, 1, 1024), clock);
		for (int i = 0; i < 10; i++) {
			assertThat(send("POST", "/api/games", "10.0.0.1").getStatus()).isEqualTo(200);
		}

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/games/x/guesses");
		request.setContent(new byte[2048]);
		assertThat(send(request).getStatus()).isEqualTo(413);
	}
}
