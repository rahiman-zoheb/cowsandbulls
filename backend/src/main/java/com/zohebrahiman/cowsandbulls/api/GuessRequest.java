package com.zohebrahiman.cowsandbulls.api;

import java.util.Locale;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Case and surrounding whitespace don't matter to the game, so they're
 * normalised before validation rather than rejected.
 */
public record GuessRequest(
		@Schema(description = "Four letters; the server ignores case and surrounding spaces", example = "fork")
		@NotNull(message = "A guess is required")
		@Pattern(regexp = "^[a-z]{4}$", message = "A guess must be exactly four letters")
		String name) {

	public GuessRequest {
		name = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
	}
}
