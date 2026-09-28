package com.zohebrahiman.cowsandbulls.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Calculator is the whole game. It had no tests at all.
 *
 * <p>Cows and bulls are returned as {@code int[]{cows, bulls}}.
 */
class CalculatorTest {

	private static int cows(String secret, String guess) {
		return Calculator.calculate(secret, guess)[0];
	}

	private static int bulls(String secret, String guess) {
		return Calculator.calculate(secret, guess)[1];
	}

	@Nested
	@DisplayName("scoring")
	class Scoring {

		@Test
		void exactMatchIsAllBulls() {
			assertThat(Calculator.calculate("abcd", "abcd")).containsExactly(0, 4);
		}

		@Test
		void fullyReversedMatchIsAllCows() {
			assertThat(Calculator.calculate("abcd", "dcba")).containsExactly(4, 0);
		}

		@Test
		void noSharedLettersScoresNothing() {
			assertThat(Calculator.calculate("abcd", "wxyz")).containsExactly(0, 0);
		}

		@ParameterizedTest(name = "secret={0} guess={1} -> {2} cows, {3} bulls")
		@CsvSource({
				"abcd, abcd, 0, 4",
				"abcd, abdc, 2, 2",
				"abcd, badc, 4, 0",
				"abcd, abce, 0, 3",
				"abcd, ebcd, 0, 3",
				"abcd, axyz, 0, 1",
				"abcd, xayz, 1, 0",
				"bast, tsab, 4, 0",
				"bros, bros, 0, 4",
		})
		void scoresKnownPairs(String secret, String guess, int expectedCows, int expectedBulls) {
			assertThat(Calculator.calculate(secret, guess)).containsExactly(expectedCows, expectedBulls);
		}

		@Test
		void cowsAndBullsNeverExceedTheWordLength() {
			assertThat(cows("abcd", "abdc") + bulls("abcd", "abdc")).isLessThanOrEqualTo(4);
		}
	}

	@Nested
	@DisplayName("repeated letters in the guess")
	class RepeatedLetters {

		// Secrets never repeat a letter (WordHelper filters those out), but nothing
		// stops a player from repeating one.

		@Test
		void aRepeatedLetterIsCountedOnceWhenItIsABull() {
			// 'a' matches position 0 only; the other three must not score again.
			assertThat(Calculator.calculate("abcd", "aaaa")).containsExactly(0, 1);
		}

		@Test
		void aRepeatedLetterIsCountedOnceWhenItIsACow() {
			// 'a' belongs at position 0, so one cow and no double counting.
			assertThat(Calculator.calculate("abcd", "xaay")).containsExactly(1, 0);
		}

		@Test
		void aBullConsumesTheLetterBeforeCowsAreCounted() {
			// 'b' is a bull at position 1, so the leading 'b' must not also be a cow.
			assertThat(Calculator.calculate("abcd", "bbcd")).containsExactly(0, 3);
		}
	}

	@Nested
	@DisplayName("input the backend does not defend against")
	class UnguardedInput {

		// Characterisation tests. These pin the behaviour that makes client-side
		// validation mandatory: every one of these surfaces as an opaque HTTP 500,
		// because there is no validation layer and no exception handler. When
		// validation is added, these should become assertions about a 400.

		@Test
		void aGuessOfTheWrongLengthThrows() {
			assertThatThrownBy(() -> Calculator.calculate("abcd", "abc"))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("not same as the secret length");

			assertThatThrownBy(() -> Calculator.calculate("abcd", "abcde"))
					.isInstanceOf(RuntimeException.class);
		}

		@Test
		void anUppercaseGuessThrowsArrayIndexOutOfBounds() {
			// charAt(i) - 'a' goes negative for uppercase and indexes int[26].
			assertThatThrownBy(() -> Calculator.calculate("abcd", "ABCD"))
					.isInstanceOf(ArrayIndexOutOfBoundsException.class);
		}

		@Test
		void aGuessWithDigitsOrSymbolsThrowsArrayIndexOutOfBounds() {
			assertThatThrownBy(() -> Calculator.calculate("abcd", "12cd"))
					.isInstanceOf(ArrayIndexOutOfBoundsException.class);

			assertThatThrownBy(() -> Calculator.calculate("abcd", "ab d"))
					.isInstanceOf(ArrayIndexOutOfBoundsException.class);
		}

		@Test
		void aNullGuessThrows() {
			assertThatThrownBy(() -> Calculator.calculate("abcd", null))
					.isInstanceOf(NullPointerException.class);
		}

		@Test
		void aNonDictionaryWordIsScoredNormally() {
			// There is no dictionary check on guesses; this is accepted today.
			assertThat(Calculator.calculate("abcd", "zzzz")).containsExactly(0, 0);
		}
	}
}
