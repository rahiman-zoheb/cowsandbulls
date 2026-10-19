package com.zohebrahiman.cowsandbulls.service;

/**
 * The cap on live games is reached. Expected only under abuse, since idle games
 * expire and creation is rate limited per client.
 */
public class TooManyGamesException extends RuntimeException {

	public TooManyGamesException() {
		super("Too many games are in progress right now; try again in a few minutes");
	}
}
