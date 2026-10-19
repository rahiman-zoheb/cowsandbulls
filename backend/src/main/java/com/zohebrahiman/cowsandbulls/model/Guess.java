package com.zohebrahiman.cowsandbulls.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * A scored guess. Created only through {@link Game#guess(String)}.
 */
@Entity
public class Guess {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(nullable = false, length = Game.WORD_LENGTH)
	private String name;

	private int cows;

	private int bulls;

	protected Guess() {
		// for JPA
	}

	Guess(Game game, String name, int cows, int bulls) {
		this.game = game;
		this.name = name;
		this.cows = cows;
		this.bulls = bulls;
	}

	public String getName() {
		return name;
	}

	public int getCows() {
		return cows;
	}

	public int getBulls() {
		return bulls;
	}
}
