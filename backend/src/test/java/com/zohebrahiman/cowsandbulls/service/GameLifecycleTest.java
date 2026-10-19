package com.zohebrahiman.cowsandbulls.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zohebrahiman.cowsandbulls.api.GameResponse;
import com.zohebrahiman.cowsandbulls.model.GameRepository;
import com.zohebrahiman.cowsandbulls.support.MutableClock;

/**
 * Expiry (#11) and the cap on live games, against the real database so the
 * ON DELETE CASCADE on guesses is exercised too.
 */
@SpringBootTest(properties = { "cowsandbulls.games.ttl=24h", "cowsandbulls.games.max-active=3" })
class GameLifecycleTest {

	@TestConfiguration
	static class Clocks {
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(Instant.parse("2026-10-18T12:00:00Z"));
		}
	}

	@Autowired
	private GameService service;

	@Autowired
	private GameCleanup cleanup;

	@Autowired
	private GameRepository games;

	@Autowired
	private MutableClock clock;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void emptyTheDatabase() {
		games.deleteAll();
	}

	private String secretOf(UUID id) {
		return jdbc.queryForObject("select secret from game where id = ?", String.class, id);
	}

	private void guessAMiss(UUID id) {
		service.guess(id, secretOf(id).equals("bros") ? "brow" : "bros");
	}

	private int guessRows() {
		return jdbc.queryForObject("select count(*) from guess", Integer.class);
	}

	@Test
	void aGameIdleLongerThanTheTtlIsDeletedWithItsGuesses() {
		UUID id = service.create().id();
		guessAMiss(id);
		assertThat(guessRows()).isEqualTo(1);

		clock.advance(Duration.ofHours(25));
		int deleted = cleanup.deleteExpiredGames();

		assertThat(deleted).isEqualTo(1);
		assertThatThrownBy(() -> service.get(id)).isInstanceOf(GameNotFoundException.class);
		assertThat(guessRows()).as("guesses removed by ON DELETE CASCADE").isZero();
	}

	@Test
	void aGameStillWithinTheTtlIsKept() {
		UUID id = service.create().id();

		clock.advance(Duration.ofHours(23));
		cleanup.deleteExpiredGames();

		assertThat(service.get(id).id()).isEqualTo(id);
	}

	@Test
	void guessingKeepsAGameAlive() {
		UUID id = service.create().id();
		clock.advance(Duration.ofHours(20));
		guessAMiss(id);

		clock.advance(Duration.ofHours(20)); // 40h after creation, 20h after the guess
		cleanup.deleteExpiredGames();

		assertThat(service.get(id).guesses()).hasSize(1);
	}

	@Test
	void onlyIdleGamesAreDeleted() {
		UUID old = service.create().id();
		clock.advance(Duration.ofHours(25));
		UUID fresh = service.create().id();

		cleanup.deleteExpiredGames();

		assertThatThrownBy(() -> service.get(old)).isInstanceOf(GameNotFoundException.class);
		assertThat(service.get(fresh).id()).isEqualTo(fresh);
	}

	@Test
	void creatingPastTheCapIsRefusedUntilGamesExpire() {
		for (int i = 0; i < 3; i++) {
			service.create();
		}

		assertThatThrownBy(service::create).isInstanceOf(TooManyGamesException.class);

		clock.advance(Duration.ofHours(25));
		cleanup.deleteExpiredGames();
		GameResponse after = service.create();
		assertThat(after.id()).isNotNull();
	}
}
