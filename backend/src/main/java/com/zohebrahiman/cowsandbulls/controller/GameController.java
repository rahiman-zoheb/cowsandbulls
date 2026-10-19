package com.zohebrahiman.cowsandbulls.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zohebrahiman.cowsandbulls.core.WordHelper;
import com.zohebrahiman.cowsandbulls.model.Game;
import com.zohebrahiman.cowsandbulls.model.GameRepository;
import com.zohebrahiman.cowsandbulls.model.GuessRepository;
import com.zohebrahiman.cowsandbulls.model.Secret;
import com.zohebrahiman.cowsandbulls.model.SecretRepository;

@RestController
@RequestMapping("/api")
public class GameController {
	private final Logger log = LoggerFactory.getLogger(GameController.class);

	private SecretRepository secretRepository;
	private GuessRepository guessRepository;
	private GameRepository gameRepository;
	
	public GameController(SecretRepository secretRepository, GuessRepository guessRepository,GameRepository gameRepository) {
		this.secretRepository = secretRepository;
		this.guessRepository = guessRepository;
		this.gameRepository = gameRepository;
	}

	@GetMapping("/game/{id}")
	ResponseEntity<?> getGame(@PathVariable Long id) {
		Optional<Game> game = gameRepository.findById(id);
		return game.map(response -> ResponseEntity.ok().body(response))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/game")
	ResponseEntity<Game> createGame() throws URISyntaxException, IllegalAccessException {
		log.info("Request to create game");

		// Delete all existing List of Guesses
		this.guessRepository.deleteAll();
		// Create a new Secret
		String secretName = WordHelper.getRandomSecret();
		Secret secret = secretRepository.save(new Secret(secretName));
		// Create a new Game
		Game game = Game.builder().secret(secret).build();
		Game result = gameRepository.save(game);
		log.info("Created game {}", result);
		return ResponseEntity.created(new URI("/api/game/" + result.getId())).body(result);
	}

	@DeleteMapping("/game/{id}")
	public ResponseEntity<?> deleteGame(@PathVariable Long id) {
		log.info("Request to delete game: {}", id);
		gameRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}
}
