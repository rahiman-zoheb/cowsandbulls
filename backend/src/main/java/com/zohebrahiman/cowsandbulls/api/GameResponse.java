package com.zohebrahiman.cowsandbulls.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.zohebrahiman.cowsandbulls.model.Game;
import com.zohebrahiman.cowsandbulls.model.GameStatus;
import com.zohebrahiman.cowsandbulls.model.Guess;

/**
 * Everything a client may know about a game. There is no secret field:
 * {@code answer} is null until the game is won.
 */
public record GameResponse(UUID id, GameStatus status, List<GuessResponse> guesses, String answer) {

	public static GameResponse of(Game game) {
		List<Guess> made = game.getGuesses();
		List<GuessResponse> guesses = IntStream.range(0, made.size())
				.mapToObj(i -> GuessResponse.of(i + 1, made.get(i)))
				.toList();
		return new GameResponse(game.getId(), game.getStatus(), guesses, game.revealedSecret().orElse(null));
	}
}
