package com.zohebrahiman.cowsandbulls.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * #48: openapi.json at the repository root is the API contract the frontend
 * is checked against. It's generated from the controller and DTOs, and this
 * test fails when the two drift apart. After changing the API, regenerate it:
 *
 * <pre>./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true</pre>
 *
 * springdoc is a test dependency only, so the running app has no
 * /v3/api-docs endpoint.
 */
@SpringBootTest(properties = {
		"cowsandbulls.api.rate-limits-enabled=false",
		"springdoc.api-docs.version=openapi_3_1",
		// Without this, every @ExceptionHandler's status is listed on every
		// operation; the controller declares which errors each one can return.
		"springdoc.override-with-generic-response=false",
		"springdoc.writer-with-order-by-keys=true",
		"springdoc.default-produces-media-type=application/json",
})
@AutoConfigureMockMvc
class OpenApiSpecTest {

	static final Path SPEC = Path.of("..", "openapi.json");

	private static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

	@Autowired
	private MockMvc mockMvc;

	@TestConfiguration
	static class Metadata {

		@Bean
		OpenAPI cowsAndBullsApi() {
			// A relative server, so the spec doesn't depend on where tests run.
			return new OpenAPI()
					.info(new Info().title("Cows and Bulls API").version("1")
							.description("Guess the server's secret four-letter word in 20 tries."))
					.servers(List.of(new Server().url("/")));
		}
	}

	@Test
	void theCommittedSpecMatchesTheCode() throws Exception {
		JsonNode generated = generate();

		if (Boolean.getBoolean("openapi.update")) {
			Files.writeString(SPEC, JSON.writeValueAsString(generated) + "\n", StandardCharsets.UTF_8);
		}

		assertThat(Files.exists(SPEC)).as("%s exists", SPEC.toAbsolutePath().normalize()).isTrue();
		assertThat(JSON.readTree(Files.readString(SPEC)))
				.as("openapi.json is out of date; regenerate it with ./mvnw test -Dtest=OpenApiSpecTest -Dopenapi.update=true")
				.isEqualTo(generated);
	}

	@Test
	void theSpecDescribesTheWholeApiAndNothingElse() throws Exception {
		JsonNode paths = generate().get("paths");

		assertThat(paths.propertyNames()).containsExactlyInAnyOrder(
				"/api/games", "/api/games/{id}", "/api/games/{id}/guesses");
		assertThat(paths.get("/api/games").propertyNames()).containsExactly("post");
		assertThat(paths.get("/api/games/{id}").propertyNames()).containsExactly("get");
		assertThat(paths.get("/api/games/{id}/guesses").propertyNames()).containsExactly("post");
		// No internal types (entities, Spring's ProblemDetail) leak into it.
		assertThat(generate().at("/components/schemas").propertyNames())
				.containsExactlyInAnyOrder("GameResponse", "GuessRequest", "GuessResponse", "Problem");
	}

	@Test
	void everyErrorIsAProblemWithADetail() throws Exception {
		JsonNode paths = generate().get("paths");
		int errors = 0;
		for (JsonNode operations : paths.values()) {
			for (JsonNode operation : operations.values()) {
				for (var response : operation.get("responses").properties()) {
					if (response.getKey().startsWith("2")) {
						continue;
					}
					errors++;
					assertThat(response.getValue().at("/content/application~1problem+json/schema/$ref").asString())
							.as("%s %s", operation.get("operationId").asString(), response.getKey())
							.isEqualTo("#/components/schemas/Problem");
				}
			}
		}
		assertThat(errors).isEqualTo(12);
		assertThat(generate().at("/components/schemas/Problem/required").values()).extracting(JsonNode::asString)
				.containsExactlyInAnyOrder("title", "status", "detail");
	}

	@Test
	void theAnswerIsNullableAndThereIsNoSecretField() throws Exception {
		JsonNode game = generate().at("/components/schemas/GameResponse");

		assertThat(game.get("properties").propertyNames())
				.containsExactlyInAnyOrder("id", "status", "guesses", "attemptsLeft", "answer");
		assertThat(game.at("/properties/answer/type").values()).extracting(JsonNode::asString)
				.containsExactlyInAnyOrder("string", "null");
		assertThat(game.get("required").values()).extracting(JsonNode::asString)
				.containsExactlyInAnyOrder("id", "status", "guesses", "attemptsLeft", "answer");
	}

	private JsonNode generate() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return JSON.readTree(body);
	}
}
