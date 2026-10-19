package com.zohebrahiman.cowsandbulls.config;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Per-client limits on /api, applied before any request reaches a controller (#12).
 *
 * <ul>
 * <li>413 for a body over {@code max-request-bytes}, and 411 for a chunked body
 * whose size can't be known up front. A guess is a few bytes.</li>
 * <li>429 with Retry-After once a client exceeds {@code requests-per-minute}, or
 * {@code games-per-minute} for POST /api/games, which is what allocates
 * server memory.</li>
 * </ul>
 *
 * The client is the remote address, which Tomcat derives from X-Forwarded-For
 * only for trusted (private-network) proxies; see application.properties.
 * Windows are fixed per minute, so a client can burst up to twice the limit
 * across a boundary. That's acceptable for abuse prevention and much simpler
 * than a sliding window.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiRequestLimitFilter extends OncePerRequestFilter {

	private final ApiLimits limits;
	private final Clock clock;
	private final MinuteCounter requests = new MinuteCounter();
	private final MinuteCounter gameCreations = new MinuteCounter();

	public ApiRequestLimitFilter(ApiLimits limits, Clock clock) {
		this.limits = limits;
		this.clock = clock;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long length = request.getContentLengthLong();
		if (length > limits.maxRequestBytes()) {
			reject(response, HttpStatus.CONTENT_TOO_LARGE, "Request body is larger than " + limits.maxRequestBytes() + " bytes", null);
			return;
		}
		if (length < 0 && "chunked".equalsIgnoreCase(request.getHeader(HttpHeaders.TRANSFER_ENCODING))) {
			reject(response, HttpStatus.LENGTH_REQUIRED, "Send a Content-Length; chunked request bodies aren't accepted", null);
			return;
		}

		if (limits.rateLimitsEnabled()) {
			long minute = currentMinute();
			String client = request.getRemoteAddr();
			if (isGameCreation(request) && !gameCreations.tryAcquire(client, minute, limits.gamesPerMinute())) {
				tooMany(response, "Too many new games; try again in a minute");
				return;
			}
			if (!requests.tryAcquire(client, minute, limits.requestsPerMinute())) {
				tooMany(response, "Too many requests; try again in a minute");
				return;
			}
		}

		chain.doFilter(request, response);
	}

	/** Drops counters from finished minutes, so the maps don't grow with every client ever seen. */
	@Scheduled(fixedDelay = 60_000)
	void evictExpiredWindows() {
		long minute = currentMinute();
		requests.evictBefore(minute);
		gameCreations.evictBefore(minute);
	}

	int trackedClients() {
		return requests.size() + gameCreations.size();
	}

	private long currentMinute() {
		return clock.instant().getEpochSecond() / 60;
	}

	private static boolean isGameCreation(HttpServletRequest request) {
		return "POST".equals(request.getMethod()) && "/api/games".equals(request.getRequestURI());
	}

	private void tooMany(HttpServletResponse response, String detail) throws IOException {
		long secondsLeft = 60 - (clock.instant().getEpochSecond() % 60);
		reject(response, HttpStatus.TOO_MANY_REQUESTS, detail, String.valueOf(secondsLeft));
	}

	// The detail strings are fixed text with no client input, so no escaping
	// is needed to build the JSON.
	private static void reject(HttpServletResponse response, HttpStatus status, String detail, String retryAfter)
			throws IOException {
		response.setStatus(status.value());
		if (retryAfter != null) {
			response.setHeader(HttpHeaders.RETRY_AFTER, retryAfter);
		}
		response.setContentType("application/problem+json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value()
				+ ",\"detail\":\"" + detail + "\"}");
	}

	/** Request counts per client for the current one-minute window. */
	static final class MinuteCounter {

		private final Map<String, Window> windows = new ConcurrentHashMap<>();

		boolean tryAcquire(String client, long minute, int limit) {
			Window window = windows.compute(client,
					(key, existing) -> existing == null || existing.minute != minute ? new Window(minute) : existing);
			return window.count.incrementAndGet() <= limit;
		}

		void evictBefore(long minute) {
			windows.values().removeIf(window -> window.minute < minute);
		}

		int size() {
			return windows.size();
		}

		private record Window(long minute, AtomicInteger count) {
			Window(long minute) {
				this(minute, new AtomicInteger());
			}
		}
	}
}
