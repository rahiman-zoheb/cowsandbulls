package com.zohebrahiman.cowsandbulls.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Browser security headers on every response, now that the app serves HTML
 * as well as JSON (#46).
 *
 * The Content-Security-Policy allows only same-origin scripts, styles and
 * connections. The Vite build has no inline scripts, styles or style
 * attributes; React applies inline styles through the CSSOM, which CSP doesn't
 * restrict. Images may also be data: URIs, because Bootstrap embeds its SVG
 * icons that way.
 *
 * HSTS is left to whatever terminates TLS in front of the app.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

	static final String CONTENT_SECURITY_POLICY = String.join("; ",
			"default-src 'self'",
			"img-src 'self' data:",
			"object-src 'none'",
			"base-uri 'self'",
			"form-action 'self'",
			"frame-ancestors 'none'");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
		// Older browsers that ignore frame-ancestors.
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("Referrer-Policy", "no-referrer");
		chain.doFilter(request, response);
	}
}
