package com.zohebrahiman.cowsandbulls.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class GameTest {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	@Test
	void aNewGameIsInProgressWithNoGuessesAndHidesItsSecret() {
		Game game = new Game("bros", NOW);

		assertThat(game.getId()).isNotNull();
		assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(game.getGuesses()).isEmpty();
		assertThat(game.revealedSecret()).isEmpty();
		assertThat(game.getCreatedAt()).isNotNull();
	}

	@Test
	void eachGameGetsItsOwnRandomId() {
		assertThat(new Game("bros", NOW).getId()).isNotEqualTo(new Game("bros", NOW).getId());
	}

	@Test
	void aGuessIsScoredAndRecordedInOrder() {
		Game game = new Game("bros", NOW);

		Guess first = game.guess("sorb", NOW);
		game.guess("brow", NOW);

		assertThat(first.getName()).isEqualTo("sorb");
		assertThat(first.getCows()).isEqualTo(4);
		assertThat(first.getBulls()).isEqualTo(0);
		assertThat(game.getGuesses()).extracting(Guess::getName).containsExactly("sorb", "brow");
		assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(game.revealedSecret()).isEmpty();
	}

	@Test
	void fourBullsWinsAndRevealsTheSecret() {
		Game game = new Game("bros", NOW);

		game.guess("bros", NOW);

		assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
		assertThat(game.revealedSecret()).contains("bros");
	}

	@Test
	void noGuessesAreAcceptedOnceTheGameIsWon() {
		Game game = new Game("bros", NOW);
		game.guess("bros", NOW);

		assertThatThrownBy(() -> game.guess("brow", NOW)).isInstanceOf(GameOverException.class)
				.hasMessageContaining(game.getId().toString());
		assertThat(game.getGuesses()).hasSize(1);
	}

	@Test
	void repeatingAGuessIsRejectedAndDoesNotCostAnAttempt() {
		Game game = new Game("bros", NOW);
		game.guess("brow", NOW);

		assertThatThrownBy(() -> game.guess("brow", NOW)).isInstanceOf(DuplicateGuessException.class)
				.hasMessage("You already guessed 'brow'");
		assertThat(game.getGuesses()).hasSize(1);
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS - 1);
	}

	@Test
	void runningOutOfAttemptsLosesAndRevealsTheSecret() {
		Game game = new Game("bros", NOW);
		for (int i = 0; i < Game.MAX_ATTEMPTS; i++) {
			assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
			game.guess(miss(i), NOW);
		}

		assertThat(game.getStatus()).isEqualTo(GameStatus.LOST);
		assertThat(game.revealedSecret()).contains("bros");
		assertThat(game.attemptsLeft()).isZero();
		assertThatThrownBy(() -> game.guess("brow", NOW)).isInstanceOf(GameOverException.class);
	}

	@Test
	void aWinOnTheLastAttemptIsAWinNotALoss() {
		Game game = new Game("bros", NOW);
		for (int i = 0; i < Game.MAX_ATTEMPTS - 1; i++) {
			game.guess(miss(i), NOW);
		}

		game.guess("bros", NOW);

		assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
	}

	@Test
	void attemptsLeftCountsDownAndIsZeroOnceFinished() {
		Game game = new Game("bros", NOW);
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS);

		game.guess("brow", NOW);
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS - 1);

		game.guess("bros", NOW);
		assertThat(game.attemptsLeft()).isZero();
	}

	/** Distinct four-letter strings with no letters from "bros", so none can win. */
	private static String miss(int i) {
		String letters = "acdefghijklmnpqtuvwxyz";
		return "" + letters.charAt(i) + letters.charAt((i + 1) % letters.length())
				+ letters.charAt((i + 2) % letters.length()) + letters.charAt((i + 3) % letters.length());
	}

	@Test
	void aGuessRecordsActivitySoActiveGamesDontExpire() {
		Game game = new Game("bros", NOW);
		assertThat(game.getCreatedAt()).isEqualTo(NOW);
		assertThat(game.getLastActivityAt()).isEqualTo(NOW);

		Instant later = NOW.plusSeconds(3600);
		game.guess("brow", later);

		assertThat(game.getLastActivityAt()).isEqualTo(later);
		assertThat(game.getCreatedAt()).isEqualTo(NOW);
	}

	@Test
	void theGuessListCannotBeModifiedFromOutside() {
		Game game = new Game("bros", NOW);
		game.guess("brow", NOW);

		assertThatThrownBy(() -> game.getGuesses().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
