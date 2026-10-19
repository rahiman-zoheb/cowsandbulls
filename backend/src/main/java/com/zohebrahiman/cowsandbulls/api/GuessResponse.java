package com.zohebrahiman.cowsandbulls.api;

import com.zohebrahiman.cowsandbulls.model.Guess;

/**
 * {@code attempt} is the 1-based position in the game; it replaces the
 * database ID, which isn't the client's business.
 */
public record GuessResponse(int attempt, String name, int cows, int bulls) {

	static GuessResponse of(int attempt, Guess guess) {
		return new GuessResponse(attempt, guess.getName(), guess.getCows(), guess.getBulls());
	}
}
