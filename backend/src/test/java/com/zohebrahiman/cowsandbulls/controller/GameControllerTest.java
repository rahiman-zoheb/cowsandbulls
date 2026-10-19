package com.zohebrahiman.cowsandbulls.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.zohebrahiman.cowsandbulls.core.Calculator;
import com.zohebrahiman.cowsandbulls.model.Game;

// Creates dozens of games per run, far past the per-client limits; those are
// covered by ApiRequestLimitFilterTest and RequestLimitsIntegrationTest.
@SpringBootTest(properties = "cowsandbulls.api.rate-limits-enabled=false")
@AutoConfigureMockMvc
class GameControllerTest {

	@Autowired
	private MockMvc mockMvc;

	// The API never hands out the secret before the game ends, so tests that
	// need it read it from the database, the same place the server does.
	@Autowired
	private JdbcTemplate jdbc;

	private static List<String> dictionary;

	@BeforeAll
	static void loadDictionary() throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				GameControllerTest.class.getResourceAsStream("/four-letters.txt"), StandardCharsets.UTF_8))) {
			dictionary = reader.lines().map(String::trim).filter(w -> w.matches("[a-z]{4}")).sorted().toList();
		}
	}

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
		return guessWithBody(gameId, "{\"name\":\"" + word + "\"}");
	}

	private ResultActions guessWithBody(String gameId, String body) throws Exception {
		return mockMvc.perform(post("/api/games/{id}/guesses", gameId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	/** Real dictionary words that aren't the secret, so none of them can win. */
	private static List<String> missesFor(String secret, int count) {
		return dictionary.stream().filter(word -> !word.equals(secret)).limit(count).toList();
	}

	private static String missFor(String secret) {
		return missesFor(secret, 1).get(0);
	}

	@Nested
	@DisplayName("the answer stays on the server")
	class SecretHiding {

		@Test
		void creatingAGameReturnsOnlyTheExpectedFieldsAndNoAnswer() throws Exception {
			String body = mockMvc.perform(post("/api/games"))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/games/[0-9a-f-]{36}")))
					.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
					.andExpect(jsonPath("$.guesses").isEmpty())
					.andExpect(jsonPath("$.attemptsLeft").value(Game.MAX_ATTEMPTS))
					.andExpect(jsonPath("$.answer").doesNotExist())
					.andReturn().getResponse().getContentAsString();

			// Structural check rather than "the body doesn't contain the word":
			// a substring test would trip on field names like "status" whenever
			// the secret happened to be "stat". Any new field (say, "secret")
			// fails this until someone decides deliberately.
			Map<String, Object> game = JsonPath.read(body, "$");
			assertThat(game).containsOnlyKeys("id", "status", "guesses", "attemptsLeft", "answer");
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

			guess(id, missFor(secretOf(id)))
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
			String miss = missFor(secret);
			int[] expected = Calculator.calculate(secret, miss);

			guess(id, miss)
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", "/api/games/" + id))
					.andExpect(jsonPath("$.guesses.length()").value(1))
					.andExpect(jsonPath("$.guesses[0].attempt").value(1))
					.andExpect(jsonPath("$.guesses[0].name").value(miss))
					.andExpect(jsonPath("$.guesses[0].cows").value(expected[0]))
					.andExpect(jsonPath("$.guesses[0].bulls").value(expected[1]))
					.andExpect(jsonPath("$.attemptsLeft").value(Game.MAX_ATTEMPTS - 1));
		}

		@Test
		void caseAndSurroundingWhitespaceDontMatter() throws Exception {
			String id = startGame();
			String miss = missFor(secretOf(id));

			guess(id, "  " + miss.toUpperCase() + " ")
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.guesses[0].name").value(miss));
		}

		@Test
		void aWordWithRepeatedLettersIsALegalGuess() throws Exception {
			// Secrets never repeat a letter, but guesses may.
			guess(startGame(), "book").andExpect(status().isCreated());
		}

		@Test
		void guessingTheWordWinsAndRevealsIt() throws Exception {
			String id = startGame();
			String secret = secretOf(id);

			guess(id, secret)
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.status").value("WON"))
					.andExpect(jsonPath("$.answer").value(secret))
					.andExpect(jsonPath("$.attemptsLeft").value(0))
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

			guess(id, missFor(secret))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("Game " + id + " is already finished"));

			mockMvc.perform(get("/api/games/{id}", id)).andExpect(jsonPath("$.guesses.length()").value(1));
		}

		@Test
		void runningOutOfAttemptsLosesAndRevealsTheAnswer() throws Exception {
			String id = startGame();
			String secret = secretOf(id);
			List<String> misses = missesFor(secret, Game.MAX_ATTEMPTS + 1);

			for (int i = 0; i < Game.MAX_ATTEMPTS - 1; i++) {
				guess(id, misses.get(i)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
			}
			guess(id, misses.get(Game.MAX_ATTEMPTS - 1))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.status").value("LOST"))
					.andExpect(jsonPath("$.answer").value(secret))
					.andExpect(jsonPath("$.attemptsLeft").value(0));

			// The limit also bounds how many rows one game can create.
			guess(id, misses.get(Game.MAX_ATTEMPTS)).andExpect(status().isConflict());
			mockMvc.perform(get("/api/games/{id}", id)).andExpect(jsonPath("$.guesses.length()").value(Game.MAX_ATTEMPTS));
		}

		@Test
		void attemptsAreNumberedInOrder() throws Exception {
			String id = startGame();
			List<String> misses = missesFor(secretOf(id), 2);
			guess(id, misses.get(0));
			guess(id, misses.get(1));

			mockMvc.perform(get("/api/games/{id}", id))
					.andExpect(jsonPath("$.guesses[0].attempt").value(1))
					.andExpect(jsonPath("$.guesses[1].attempt").value(2));
		}
	}

	@Nested
	@DisplayName("rejected guesses")
	class Rejected {

		@ParameterizedTest(name = "\"{0}\"")
		@ValueSource(strings = { "abc", "abcde", "ab1d", "ab d", "ab-d", "", "über" })
		void aMalformedGuessIs400WithAReasonAndIsNotRecorded(String word) throws Exception {
			// Previously every one of these reached Calculator and came back as
			// an opaque 500 (length mismatch, or an int[26] index out of range).
			String id = startGame();

			guess(id, word)
					.andExpect(status().isBadRequest())
					.andExpect(header().string("Content-Type", "application/problem+json"))
					.andExpect(jsonPath("$.detail").value("A guess must be exactly four letters"));

			mockMvc.perform(get("/api/games/{id}", id)).andExpect(jsonPath("$.guesses").isEmpty());
		}

		@Test
		void aMissingGuessIs400() throws Exception {
			guessWithBody(startGame(), "{}")
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail").value("A guess is required"));
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = { "not json", "[\"word\"]", "" })
		void aBodyThatIsntAGuessObjectIs400(String body) throws Exception {
			guessWithBody(startGame(), body)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("{\"name\":\"word\"}")));
		}

		@Test
		void aWellFormedNonWordIs422() throws Exception {
			String id = startGame();

			guess(id, "qqqq")
					.andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.detail").value("'qqqq' is not in the word list"));

			mockMvc.perform(get("/api/games/{id}", id)).andExpect(jsonPath("$.guesses").isEmpty());
		}

		@Test
		void repeatingAGuessIs409AndDoesNotCostAnAttempt() throws Exception {
			String id = startGame();
			String miss = missFor(secretOf(id));
			guess(id, miss);

			guess(id, miss.toUpperCase())
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("You already guessed '" + miss + "'"));

			mockMvc.perform(get("/api/games/{id}", id))
					.andExpect(jsonPath("$.guesses.length()").value(1))
					.andExpect(jsonPath("$.attemptsLeft").value(Game.MAX_ATTEMPTS - 1));
		}

		@Test
		void aFinishedGameIsReportedBeforeAnUnknownWord() throws Exception {
			String id = startGame();
			guess(id, secretOf(id));

			guess(id, "qqqq").andExpect(status().isConflict());
		}
	}

	@Nested
	@DisplayName("logging")
	@ExtendWith(OutputCaptureExtension.class)
	class Logging {

		@Test
		void aGuessContainingLineBreaksCannotForgeLogLines(CapturedOutput output) throws Exception {
			// #13: the old controller logged the raw request body, so CR/LF in a
			// guess wrote an attacker-chosen line into the server log.
			String forged = "2026-10-18 INFO FORGED: admin logged in";

			guessWithBody(startGame(), "{\"name\":\"abcd\\r\\n" + forged + "\"}")
					.andExpect(status().isBadRequest());

			// Case-insensitive: GuessRequest lowercases its input, so anything that
			// logged the value would write the forged line in lowercase.
			assertThat(output.getAll().toLowerCase()).doesNotContain(forged.toLowerCase());
		}
	}

	@Nested
	@DisplayName("games are independent")
	class Isolation {

		@Test
		void aGuessInOneGameDoesNotAppearInAnother() throws Exception {
			String a = startGame();
			String b = startGame();

			guess(a, missFor(secretOf(a)));

			mockMvc.perform(get("/api/games/{id}", a)).andExpect(jsonPath("$.guesses.length()").value(1));
			mockMvc.perform(get("/api/games/{id}", b)).andExpect(jsonPath("$.guesses.length()").value(0));
		}

		@Test
		void startingANewGameLeavesOtherGamesAlone() throws Exception {
			// The old POST /api/game ran guessRepository.deleteAll(), wiping
			// every player's guesses.
			String a = startGame();
			guess(a, missFor(secretOf(a)));

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

			guess(unknown.toString(), "fork").andExpect(status().isNotFound());
		}

		@Test
		void aMalformedGameIdIs400() throws Exception {
			mockMvc.perform(get("/api/games/{id}", "1")).andExpect(status().isBadRequest());
		}
	}
}
