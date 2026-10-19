package com.zohebrahiman.cowsandbulls.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WordListTest {

	@Test
	void everySecretIsFourLowercaseLettersWithNoRepeats() throws IOException {
		// Calculator indexes int[26] by charAt(i) - 'a', so a single bad entry
		// would make every guess in the game that drew it throw.
		assertThat(new WordList().secrets())
				.hasSize(3120)
				.allSatisfy(word -> {
					assertThat(word).matches("[a-z]{4}");
					assertThat(word.chars().distinct().count()).isEqualTo(4);
				});
	}

	@Test
	void randomSecretsComeFromTheListAndVary() throws IOException {
		WordList words = new WordList();
		Set<String> drawn = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			drawn.add(words.randomSecret());
		}

		assertThat(words.secrets()).containsAll(drawn);
		assertThat(drawn).hasSizeGreaterThan(1);
	}

	@Test
	void rejectsWordsThatWouldBreakScoring() {
		assertThat(WordList.isUsableSecret("bros")).isTrue();
		assertThat(WordList.isUsableSecret("boss")).as("repeated letter").isFalse();
		assertThat(WordList.isUsableSecret("Bros")).as("uppercase").isFalse();
		assertThat(WordList.isUsableSecret("bro")).as("too short").isFalse();
		assertThat(WordList.isUsableSecret("br0s")).as("digit").isFalse();
		assertThat(WordList.isUsableSecret("")).as("blank line").isFalse();
	}
}
