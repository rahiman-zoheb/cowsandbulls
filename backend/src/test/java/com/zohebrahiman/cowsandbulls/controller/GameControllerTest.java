package com.zohebrahiman.cowsandbulls.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class GameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	// The API never hands out the secret before a win, so tests that need it
	// read it from the database, the same place the server does.
	@Autowired
	private JdbcTemplate jdbc;

	private String startGame() throws Exception {
		String body = mockMvc.perform(post("/api/games"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private String secretOf(String gameId) {
		return jdbc.queryForObject("select secret from game where id = ?", String.class, UUID.fromString(gameId));
	}

	private ResultActions guess(String gameId, String word) throws Exception {
		return mockMvc.perform(post("/api/games/{id}/guesses", gameId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + word + "\"}"));
	}

	/** A four-letter word sharing no letters with the secret, so it can't win. */
	private static String nonWinningGuessFor(String secret) {
		for (String candidate : new String[] { "wxyz", "abcd", "efgh", "ijkl" }) {
			if (candidate.chars().noneMatch(c -> secret.indexOf(c) >= 0)) {
				return candidate;
			}
		}
		throw new IllegalStateException("no disjoint guess for " + secret);
	}

	@Nested
	@DisplayName("the answer stays on the server")
	class SecretHiding {

		@Test
		void creatingAGameReturnsOnlyIdStatusGuessesAndANullAnswer() throws Exception {
			String body = mockMvc.perform(post("/api/games"))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/games/[0-9a-f-]{36}")))
					.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
					.andExpect(jsonPath("$.guesses").isEmpty())
					.andExpect(jsonPath("$.answer").doesNotExist())
					.andReturn().getResponse().getContentAsString();

			// Structural check rather than "the body doesn't contain the word":
			// a substring test would trip on field names like "status" whenever
			// the secret happened to be "stat". Any new field (say, "secret")
			// fails this until someone decides deliberately.
			Map<String, Object> game = JsonPath.read(body, "$");
			assertThat(game).containsOnlyKeys("id", "status", "guesses", "answer");
		}

		@Test
		void readingAGameInProgressDoesNotRevealTheAnswer() throws Exception {
			String id = startGame();

			mockMvc.perform(get("/api/games/{id}", id))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(id))
					.andExpect(jsonPath("$.answer").doesNotExist());
		}

		@Test
		void aNonWinningGuessDoesNotRevealTheAnswer() throws Exception {
			String id = startGame();

			guess(id, nonWinningGuessFor(secretOf(id)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
					.andExpect(jsonPath("$.answer").doesNotExist());
		}

		@Test
		void gameIdsAreRandomUuidsNotSequentialNumbers() throws Exception {
			String first = startGame();
			String second = startGame();

			assertThat(UUID.fromString(first).version()).isEqualTo(4);
			assertThat(first).isNotEqualTo(second);
		}

		@Test
		void everyEndpointThatExposedOrChangedSecretsIsGone() throws Exception {
			mockMvc.perform(get("/api/secrets")).andExpect(status().isNotFound());
			mockMvc.perform(get("/api/secret/1")).andExpect(status().isNotFound());
			mockMvc.perform(put("/api/secret/1").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"name\":\"pwnd\"}"))
					.andExpect(status().isNotFound());
			mockMvc.perform(post("/api/secret").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"pwnd\"}"))
					.andExpect(status().isNotFound());
		}

		@Test
		void theOldGlobalEndpointsAreGone() throws Exception {
			mockMvc.perform(post("/api/game")).andExpect(status().isNotFound());
			mockMvc.perform(get("/api/guesses")).andExpect(status().isNotFound());
			mockMvc.perform(delete("/api/guess/1")).andExpect(status().isNotFound());
			mockMvc.perform(delete("/api/games/{id}", startGame())).andExpect(status().isMethodNotAllowed());
		}
	}

	@Nested
	@DisplayName("playing a game")
	class Playing {

		@Test
		void aGuessIsScoredAndReturnedWithTheUpdatedGame() throws Exception {
			String id = startGame();
			String secret = secretOf(id);
			// Reversing a secret with no repeated letters keeps every letter
			// and moves all four, so it scores exactly 4 cows and 0 bulls.
			String reversed = new StringBuilder(secret).reverse().toString();

			guess(id, reversed)
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", "/api/games/" + id))
					.andExpect(jsonPath("$.guesses.length()").value(1))
					.andExpect(jsonPath("$.guesses[0].attempt").value(1))
					.andExpect(jsonPath("$.guesses[0].name").value(reversed))
					.andExpect(jsonPath("$.guesses[0].cows").value(4))
					.andExpect(jsonPath("$.guesses[0].bulls").value(0));
		}

		@Test
		void guessingTheWordWinsAndRevealsIt() throws Exception {
			String id = startGame();
			String secret = secretOf(id);

			guess(id, secret)
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.status").value("WON"))
					.andExpect(jsonPath("$.answer").value(secret))
					.andExpect(jsonPath("$.guesses[0].bulls").value(4));

			// And it stays revealed for a finished game.
			mockMvc.perform(get("/api/games/{id}", id))
					.andExpect(jsonPath("$.status").value("WON"))
					.andExpect(jsonPath("$.answer").value(secret));
		}

		@Test
		void guessingAfterAWinIsRejected() throws Exception {
			String id = startGame();
			String secret = secretOf(id);
			guess(id, secret);

			guess(id, nonWinningGuessFor(secret))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("Game " + id + " is already finished"));

			mockMvc.perform(get("/api/games/{id}", id)).andExpect(jsonPath("$.guesses.length()").value(1));
		}

		@Test
		void attemptsAreNumberedInOrder() throws Exception {
			String id = startGame();
			String miss = nonWinningGuessFor(secretOf(id));
			guess(id, miss);
			guess(id, miss);

			mockMvc.perform(get("/api/games/{id}", id))
					.andExpect(jsonPath("$.guesses[0].attempt").value(1))
					.andExpect(jsonPath("$.guesses[1].attempt").value(2));
		}
	}

	@Nested
	@DisplayName("games are independent")
	class Isolation {

		@Test
		void aGuessInOneGameDoesNotAppearInAnother() throws Exception {
			String a = startGame();
			String b = startGame();

			guess(a, nonWinningGuessFor(secretOf(a)));

			mockMvc.perform(get("/api/games/{id}", a)).andExpect(jsonPath("$.guesses.length()").value(1));
			mockMvc.perform(get("/api/games/{id}", b)).andExpect(jsonPath("$.guesses.length()").value(0));
		}

		@Test
		void startingANewGameLeavesOtherGamesAlone() throws Exception {
			// The old POST /api/game ran guessRepository.deleteAll(), wiping
			// every player's guesses.
			String a = startGame();
			guess(a, nonWinningGuessFor(secretOf(a)));

			startGame();

			mockMvc.perform(get("/api/games/{id}", a)).andExpect(jsonPath("$.guesses.length()").value(1));
		}

		@Test
		void eachGameIsScoredAgainstItsOwnSecret() throws Exception {
			String a = startGame();
			String b = startGame();

			guess(a, secretOf(a)).andExpect(jsonPath("$.status").value("WON"));
			mockMvc.perform(get("/api/games/{id}", b)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
		}
	}

	@Nested
	@DisplayName("errors")
	class Errors {

		@Test
		void anUnknownGameIs404WithAProblemBody() throws Exception {
			UUID unknown = UUID.randomUUID();

			mockMvc.perform(get("/api/games/{id}", unknown))
					.andExpect(status().isNotFound())
					.andExpect(header().string("Content-Type", "application/problem+json"))
					.andExpect(jsonPath("$.detail").value("No game with id " + unknown));

			guess(unknown.toString(), "abcd").andExpect(status().isNotFound());
		}

		@Test
		void aMalformedGameIdIs400() throws Exception {
			mockMvc.perform(get("/api/games/{id}", "1")).andExpect(status().isBadRequest());
		}
	}
}
