package com.zohebrahiman.cowsandbulls.api;

import java.util.Locale;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Case and surrounding whitespace don't matter to the game, so they're
 * normalised before validation rather than rejected.
 */
public record GuessRequest(
		@NotNull(message = "A guess is required")
		@Pattern(regexp = "[a-z]{4}", message = "A guess must be exactly four letters")
		String name) {

	public GuessRequest {
		name = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
	}
}
