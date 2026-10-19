package com.zohebrahiman.cowsandbulls.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GameTest {

	@Test
	void aNewGameIsInProgressWithNoGuessesAndHidesItsSecret() {
		Game game = new Game("bros");

		assertThat(game.getId()).isNotNull();
		assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(game.getGuesses()).isEmpty();
		assertThat(game.revealedSecret()).isEmpty();
		assertThat(game.getCreatedAt()).isNotNull();
	}

	@Test
	void eachGameGetsItsOwnRandomId() {
		assertThat(new Game("bros").getId()).isNotEqualTo(new Game("bros").getId());
	}

	@Test
	void aGuessIsScoredAndRecordedInOrder() {
		Game game = new Game("bros");

		Guess first = game.guess("sorb");
		game.guess("brow");

		assertThat(first.getName()).isEqualTo("sorb");
		assertThat(first.getCows()).isEqualTo(4);
		assertThat(first.getBulls()).isEqualTo(0);
		assertThat(game.getGuesses()).extracting(Guess::getName).containsExactly("sorb", "brow");
		assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
		assertThat(game.revealedSecret()).isEmpty();
	}

	@Test
	void fourBullsWinsAndRevealsTheSecret() {
		Game game = new Game("bros");

		game.guess("bros");

		assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
		assertThat(game.revealedSecret()).contains("bros");
	}

	@Test
	void noGuessesAreAcceptedOnceTheGameIsWon() {
		Game game = new Game("bros");
		game.guess("bros");

		assertThatThrownBy(() -> game.guess("brow")).isInstanceOf(GameOverException.class)
				.hasMessageContaining(game.getId().toString());
		assertThat(game.getGuesses()).hasSize(1);
	}

	@Test
	void repeatingAGuessIsRejectedAndDoesNotCostAnAttempt() {
		Game game = new Game("bros");
		game.guess("brow");

		assertThatThrownBy(() -> game.guess("brow")).isInstanceOf(DuplicateGuessException.class)
				.hasMessage("You already guessed 'brow'");
		assertThat(game.getGuesses()).hasSize(1);
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS - 1);
	}

	@Test
	void runningOutOfAttemptsLosesAndRevealsTheSecret() {
		Game game = new Game("bros");
		for (int i = 0; i < Game.MAX_ATTEMPTS; i++) {
			assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
			game.guess(miss(i));
		}

		assertThat(game.getStatus()).isEqualTo(GameStatus.LOST);
		assertThat(game.revealedSecret()).contains("bros");
		assertThat(game.attemptsLeft()).isZero();
		assertThatThrownBy(() -> game.guess("brow")).isInstanceOf(GameOverException.class);
	}

	@Test
	void aWinOnTheLastAttemptIsAWinNotALoss() {
		Game game = new Game("bros");
		for (int i = 0; i < Game.MAX_ATTEMPTS - 1; i++) {
			game.guess(miss(i));
		}

		game.guess("bros");

		assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
	}

	@Test
	void attemptsLeftCountsDownAndIsZeroOnceFinished() {
		Game game = new Game("bros");
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS);

		game.guess("brow");
		assertThat(game.attemptsLeft()).isEqualTo(Game.MAX_ATTEMPTS - 1);

		game.guess("bros");
		assertThat(game.attemptsLeft()).isZero();
	}

	/** Distinct four-letter strings with no letters from "bros", so none can win. */
	private static String miss(int i) {
		String letters = "acdefghijklmnpqtuvwxyz";
		return "" + letters.charAt(i) + letters.charAt((i + 1) % letters.length())
				+ letters.charAt((i + 2) % letters.length()) + letters.charAt((i + 3) % letters.length());
	}

	@Test
	void theGuessListCannotBeModifiedFromOutside() {
		Game game = new Game("bros");
		game.guess("brow");

		assertThatThrownBy(() -> game.getGuesses().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
