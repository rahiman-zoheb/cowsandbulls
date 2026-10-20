package com.zohebrahiman.cowsandbulls.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zohebrahiman.cowsandbulls.api.GameResponse;
import com.zohebrahiman.cowsandbulls.api.GuessRequest;
import com.zohebrahiman.cowsandbulls.api.Problem;
import com.zohebrahiman.cowsandbulls.service.GameService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The whole public API. Every game is independent and addressed by an opaque
 * ID; there is no way to list games, read or change a secret, or delete
 * anything.
 *
 * The annotations only document responses for the OpenAPI spec (#48), which
 * is generated in tests; errors are RFC 9457 problem details.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

	private static final String PROBLEM = "application/problem+json";

	private final GameService games;

	public GameController(GameService games) {
		this.games = games;
	}

	@Operation(summary = "Start a game", description = "Picks a secret word on the server. The answer stays null until the game ends.")
	@ApiResponse(responseCode = "201", description = "The new game",
			headers = @Header(name = "Location", description = "The game's URL", schema = @Schema(type = "string")))
	@ApiResponse(responseCode = "429", description = "Too many games or requests from this address; see Retry-After",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "503", description = "The server is holding as many games as it allows; see Retry-After",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@PostMapping
	public ResponseEntity<GameResponse> create() {
		GameResponse game = games.create();
		return ResponseEntity.created(location(game)).body(game);
	}

	@Operation(summary = "Get a game")
	@ApiResponse(responseCode = "200", description = "The game")
	@ApiResponse(responseCode = "400", description = "The ID is not a UUID",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "404", description = "No such game, or it has expired",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "429", description = "Too many requests from this address; see Retry-After",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@GetMapping("/{id}")
	public GameResponse get(@PathVariable UUID id) {
		return games.get(id);
	}

	/**
	 * Returns the whole updated game, so the client learns the score and
	 * whether it has won from a single response.
	 */
	@Operation(summary = "Make a guess",
			description = "Returns the whole updated game, so one response gives the score and whether the game is over.")
	@ApiResponse(responseCode = "201", description = "The game, with the scored guess last")
	@ApiResponse(responseCode = "400", description = "The ID is not a UUID, or the body is not a four-letter guess",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "404", description = "No such game, or it has expired",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "409", description = "The game is over, the word was already guessed, or another guess raced this one",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "411", description = "No Content-Length; chunked bodies are refused",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "413", description = "The body is over the size limit",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "422", description = "A well-formed guess that is not in the word list",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@ApiResponse(responseCode = "429", description = "Too many requests from this address; see Retry-After",
			content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = Problem.class)))
	@PostMapping("/{id}/guesses")
	public ResponseEntity<GameResponse> guess(@PathVariable UUID id, @Valid @RequestBody GuessRequest request) {
		GameResponse game = games.guess(id, request.name());
		return ResponseEntity.created(location(game)).body(game);
	}

	private static URI location(GameResponse game) {
		return URI.create("/api/games/" + game.id());
	}
}
