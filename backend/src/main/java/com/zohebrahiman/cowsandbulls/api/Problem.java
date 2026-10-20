package com.zohebrahiman.cowsandbulls.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Documents the RFC 9457 body of every error for the OpenAPI spec (#48); it
 * is never instantiated. Spring's ProblemDetail can't stand in for it, since
 * it has Java-only properties. The request limit filter writes only title,
 * status and detail, so only those are required. Spring writes instance as
 * a path, which is a URI reference rather than an absolute URI.
 */
@Schema(name = "Problem", description = "An RFC 9457 problem detail")
public record Problem(
		@Schema(format = "uri-reference", description = "Defaults to about:blank") String type,
		@Schema(requiredMode = RequiredMode.REQUIRED) String title,
		@Schema(requiredMode = RequiredMode.REQUIRED) int status,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "What went wrong, fit to show the player") String detail,
		@Schema(format = "uri-reference", description = "The request path, such as /api/games") String instance) {
}
