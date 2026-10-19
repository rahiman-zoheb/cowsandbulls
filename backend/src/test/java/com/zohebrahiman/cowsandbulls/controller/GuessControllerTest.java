package com.zohebrahiman.cowsandbulls.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class GuessControllerTest {

	@Autowired
	private MockMvc mockMvc;

	/** Starts a new game and returns the word it is supposed to be played against. */
	private String startGameAndReadSecret() throws Exception {
		String body = mockMvc.perform(post("/api/game").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.secret.name");
	}

	private String guess(String word) throws Exception {
		return mockMvc.perform(post("/api/guess")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + word + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	@DisplayName("guessing the current game's word wins")
	void guessingTheCurrentSecretScoresFourBulls() throws Exception {
		String secret = startGameAndReadSecret();

		mockMvc.perform(post("/api/guess")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + secret + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.bulls").value(4))
				.andExpect(jsonPath("$.cows").value(0));
	}

	@Test
	@DisplayName("a new game actually changes the answer")
	void startingANewGameMovesScoringToTheNewSecret() throws Exception {
		String firstSecret = startGameAndReadSecret();
		String secondSecret = startGameAndReadSecret();

		// The regression this guards: scoring used secretRepository.findAll().get(0),
		// so the answer stayed fixed at the word seeded on startup and the first
		// game's word kept winning forever.
		assertThat(JsonPath.<Integer>read(guess(secondSecret), "$.bulls"))
				.as("the current game's word must win")
				.isEqualTo(4);

		if (!firstSecret.equals(secondSecret)) {
			assertThat(JsonPath.<Integer>read(guess(firstSecret), "$.bulls"))
					.as("the previous game's word must no longer win")
					.isNotEqualTo(4);
		}
	}

	@Test
	@DisplayName("starting a game clears the previous guesses")
	void startingAGameClearsTheGuessList() throws Exception {
		String secret = startGameAndReadSecret();
		guess(secret);

		startGameAndReadSecret();

		mockMvc.perform(get("/api/guesses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("a guess is persisted and listed")
	void submittedGuessesAreListed() throws Exception {
		startGameAndReadSecret();
		guess("abcd");

		mockMvc.perform(get("/api/guesses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("abcd"))
				.andExpect(jsonPath("$[0].id").isNumber())
				.andExpect(jsonPath("$[0].cows").isNumber())
				.andExpect(jsonPath("$[0].bulls").isNumber());
	}
}
