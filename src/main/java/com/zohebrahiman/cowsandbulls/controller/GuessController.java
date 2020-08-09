package com.zohebrahiman.cowsandbulls.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zohebrahiman.cowsandbulls.core.Calculator;
import com.zohebrahiman.cowsandbulls.model.Guess;
import com.zohebrahiman.cowsandbulls.model.GuessRepository;
import com.zohebrahiman.cowsandbulls.model.Secret;
import com.zohebrahiman.cowsandbulls.model.SecretRepository;

@RestController
@RequestMapping("/api")
public class GuessController {
	private final Logger log = LoggerFactory.getLogger(GuessController.class);

	private SecretRepository secretRepository;
	private GuessRepository guessRepository;

	public GuessController(SecretRepository secretRepository, GuessRepository guessRepository) {
		this.secretRepository = secretRepository;
		this.guessRepository = guessRepository;
	}

	@GetMapping("/guesses")
	Collection<Guess> guesss() {
		return guessRepository.findAll();
	}

	@GetMapping("/guess/{id}")
	ResponseEntity<?> getGuess(@PathVariable Long id) {
		Optional<Guess> guess = guessRepository.findById(id);
		return guess.map(response -> ResponseEntity.ok().body(response))
				.orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/guess")
	ResponseEntity<Guess> createGuess(@Valid @RequestBody Guess guess) throws URISyntaxException {
		log.info("Request to create guess: {}", guess);
		Secret secret = secretRepository.findAll().get(0);
		int[] cowsAndBulls = Calculator.calculate(secret.getName(), guess.getName());
		guess.setCows(cowsAndBulls[0]);
		guess.setBulls(cowsAndBulls[1]);

		Guess result = guessRepository.save(guess);
		log.info("Created guess: {}", guess);

		return ResponseEntity.created(new URI("/api/guess/" + result.getId())).body(result);
	}

	@DeleteMapping("/guess/{id}")
	public ResponseEntity<?> deleteGuess(@PathVariable Long id) {
		log.info("Request to delete guess: {}", id);
		guessRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}
}
