package com.zohebrahiman.cowsandbulls.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zohebrahiman.cowsandbulls.core.Calculator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;

/**
 * One player's game. The secret never leaves this class except through
 * {@link #revealedSecret()}, which only returns it once the game is over.
 */
@Entity
public class Game {

	public static final int WORD_LENGTH = 4;

	/**
	 * Generous for a four-letter word, and it also caps how many rows a single
	 * game can ever create.
	 */
	public static final int MAX_ATTEMPTS = 20;

	/**
	 * Random, so game IDs can't be enumerated the way sequential ones could.
	 */
	@Id
	private UUID id;

	// Deliberately has no getter.
	@Column(nullable = false, length = WORD_LENGTH)
	private String secret;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private GameStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	/**
	 * Two guesses racing on the same game make the second fail instead of both
	 * being applied. Also lets Spring Data tell a new game (null) from a
	 * persisted one, since the ID is assigned up front.
	 */
	@Version
	private Long version;

	@OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<Guess> guesses = new ArrayList<>();

	protected Game() {
		// for JPA
	}

	public Game(String secret) {
		this.id = UUID.randomUUID();
		this.secret = secret;
		this.status = GameStatus.IN_PROGRESS;
		this.createdAt = Instant.now();
	}

	/**
	 * Scores a guess against the secret and records it. The caller is
	 * responsible for checking the word is well-formed and in the dictionary.
	 *
	 * @throws GameOverException if the game is already won or lost
	 * @throws DuplicateGuessException if this word was already guessed
	 */
	public Guess guess(String word) {
		requireInProgress();
		if (guesses.stream().anyMatch(previous -> previous.getName().equals(word))) {
			throw new DuplicateGuessException(word);
		}
		int[] cowsAndBulls = Calculator.calculate(secret, word);
		Guess guess = new Guess(this, word, cowsAndBulls[0], cowsAndBulls[1]);
		guesses.add(guess);
		if (guess.getBulls() == WORD_LENGTH) {
			status = GameStatus.WON;
		} else if (guesses.size() >= MAX_ATTEMPTS) {
			status = GameStatus.LOST;
		}
		return guess;
	}

	/**
	 * @throws GameOverException if the game is already won or lost
	 */
	public void requireInProgress() {
		if (status != GameStatus.IN_PROGRESS) {
			throw new GameOverException(id);
		}
	}

	public int attemptsLeft() {
		return status == GameStatus.IN_PROGRESS ? MAX_ATTEMPTS - guesses.size() : 0;
	}

	public Optional<String> revealedSecret() {
		return status == GameStatus.IN_PROGRESS ? Optional.empty() : Optional.of(secret);
	}

	public UUID getId() {
		return id;
	}

	public GameStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public List<Guess> getGuesses() {
		return Collections.unmodifiableList(guesses);
	}
}
