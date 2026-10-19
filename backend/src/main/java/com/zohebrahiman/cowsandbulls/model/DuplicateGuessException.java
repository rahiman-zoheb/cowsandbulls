package com.zohebrahiman.cowsandbulls.model;

/**
 * The same word was guessed twice in one game.
 */
public class DuplicateGuessException extends RuntimeException {

	public DuplicateGuessException(String word) {
		// Safe to echo: only words that passed validation get this far.
		super("You already guessed '" + word + "'");
	}
}
