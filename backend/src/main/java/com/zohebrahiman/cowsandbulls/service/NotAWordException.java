package com.zohebrahiman.cowsandbulls.service;

/**
 * A well-formed guess that isn't in the word list.
 */
public class NotAWordException extends RuntimeException {

	public NotAWordException(String word) {
		// Safe to echo: only [a-z]{4} gets this far.
		super("'" + word + "' is not in the word list");
	}
}
