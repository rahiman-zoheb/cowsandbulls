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
import com.zohebrahiman.cowsandbulls.service.GameService;

import jakarta.validation.Valid;

/**
 * The whole public API. Every game is independent and addressed by an opaque
 * ID; there is no way to list games, read or change a secret, or delete
 * anything.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameService games;

	public GameController(GameService games) {
		this.games = games;
	}

	@PostMapping
	public ResponseEntity<GameResponse> create() {
		GameResponse game = games.create();
		return ResponseEntity.created(location(game)).body(game);
	}

	@GetMapping("/{id}")
	public GameResponse get(@PathVariable UUID id) {
		return games.get(id);
	}

	/**
	 * Returns the whole updated game, so the client learns the score and
	 * whether it has won from a single response.
	 */
	@PostMapping("/{id}/guesses")
	public ResponseEntity<GameResponse> guess(@PathVariable UUID id, @Valid @RequestBody GuessRequest request) {
		GameResponse game = games.guess(id, request.name());
		return ResponseEntity.created(location(game)).body(game);
	}

	private static URI location(GameResponse game) {
		return URI.create("/api/games/" + game.id());
	}
}
