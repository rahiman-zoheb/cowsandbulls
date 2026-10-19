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
 * {@link #revealedSecret()}, which only returns it once the game is won.
 */
@Entity
public class Game {

	public static final int WORD_LENGTH = 4;

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
	 * Scores a guess against the secret and records it.
	 *
	 * @throws GameOverException if the game has already been won
	 */
	public Guess guess(String word) {
		if (status != GameStatus.IN_PROGRESS) {
			throw new GameOverException(id);
		}
		int[] cowsAndBulls = Calculator.calculate(secret, word);
		Guess guess = new Guess(this, word, cowsAndBulls[0], cowsAndBulls[1]);
		guesses.add(guess);
		if (guess.getBulls() == WORD_LENGTH) {
			status = GameStatus.WON;
		}
		return guess;
	}

	public Optional<String> revealedSecret() {
		return status == GameStatus.WON ? Optional.of(secret) : Optional.empty();
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
