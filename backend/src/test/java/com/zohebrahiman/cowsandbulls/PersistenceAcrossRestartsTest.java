package com.zohebrahiman.cowsandbulls;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zohebrahiman.cowsandbulls.api.GameResponse;
import com.zohebrahiman.cowsandbulls.service.GameService;

/**
 * #27: with the prod profile, games live in a file database and survive the
 * application restarting. Starts the real application twice against one file.
 */
class PersistenceAcrossRestartsTest {

	@TempDir
	Path dir;

	private ConfigurableApplicationContext startProd() {
		return new SpringApplicationBuilder(CowsandbullsApplication.class)
				.web(WebApplicationType.NONE)
				.profiles("prod")
				.properties(
						"COWSANDBULLS_DB_PATH=" + dir.resolve("cowsandbulls").toAbsolutePath(),
						"COWSANDBULLS_DB_PASSWORD=test-only-password")
				.run();
	}

	@Test
	void aGameAndItsGuessesSurviveARestart() {
		UUID id;
		try (ConfigurableApplicationContext first = startProd()) {
			GameService games = first.getBean(GameService.class);
			id = games.create().id();
			String secret = first.getBean(JdbcTemplate.class)
					.queryForObject("select secret from game where id = ?", String.class, id);
			games.guess(id, secret.equals("fork") ? "bros" : "fork");
		}

		assertThat(Files.exists(dir.resolve("cowsandbulls.mv.db"))).as("database file on disk").isTrue();

		try (ConfigurableApplicationContext second = startProd()) {
			GameResponse game = second.getBean(GameService.class).get(id);

			assertThat(game.guesses()).hasSize(1);
			assertThat(game.answer()).as("still hidden after a restart").isNull();
		}
	}

	@Test
	void restartingAppliesNoMigrationTwice() {
		// Flyway records what it ran; a second start against the same file must
		// find the schema current rather than trying to create it again.
		try (ConfigurableApplicationContext first = startProd()) {
			first.getBean(GameService.class).create();
		}
		try (ConfigurableApplicationContext second = startProd()) {
			// Only SQL rows are migrations; Flyway also writes a marker row (type
			// TABLE) when it creates its history table.
			List<String> applied = second.getBean(JdbcTemplate.class).queryForList(
					"select \"script\" from \"flyway_schema_history\" where \"type\" = 'SQL' and \"success\"",
					String.class);
			assertThat(applied).containsExactly("V1__create_games_and_guesses.sql");
		}
	}
}
