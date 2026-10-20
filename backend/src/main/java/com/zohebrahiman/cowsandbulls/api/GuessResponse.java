package com.zohebrahiman.cowsandbulls.api;

import com.zohebrahiman.cowsandbulls.model.Guess;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * {@code attempt} is the 1-based position in the game; it replaces the
 * database ID, which isn't the client's business.
 */
public record GuessResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "1") int attempt,
		@Schema(requiredMode = RequiredMode.REQUIRED, pattern = "^[a-z]{4}$") String name,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0", maximum = "4",
				description = "Right letter, wrong place") int cows,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0", maximum = "4",
				description = "Right letter, right place") int bulls) {

	static GuessResponse of(int attempt, Guess guess) {
		return new GuessResponse(attempt, guess.getName(), guess.getCows(), guess.getBulls());
	}
}
