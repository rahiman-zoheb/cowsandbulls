package com.zohebrahiman.cowsandbulls.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.zohebrahiman.cowsandbulls.model.Game;
import com.zohebrahiman.cowsandbulls.model.GameStatus;
import com.zohebrahiman.cowsandbulls.model.Guess;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Everything a client may know about a game. There is no secret field:
 * {@code answer} is null until the game is won or lost.
 */
public record GameResponse(
		@Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
		@Schema(requiredMode = RequiredMode.REQUIRED) GameStatus status,
		@Schema(requiredMode = RequiredMode.REQUIRED, description = "Oldest first") List<GuessResponse> guesses,
		@Schema(requiredMode = RequiredMode.REQUIRED, minimum = "0") int attemptsLeft,
		@Schema(requiredMode = RequiredMode.REQUIRED, types = { "string", "null" }, pattern = "^[a-z]{4}$",
				description = "The secret word once the game is WON or LOST; null while it is IN_PROGRESS") String answer) {

	public static GameResponse of(Game game) {
		List<Guess> made = game.getGuesses();
		List<GuessResponse> guesses = IntStream.range(0, made.size())
				.mapToObj(i -> GuessResponse.of(i + 1, made.get(i)))
				.toList();
		return new GameResponse(game.getId(), game.getStatus(), guesses, game.attemptsLeft(),
				game.revealedSecret().orElse(null));
	}
}
