package com.zohebrahiman.cowsandbulls.model;

import java.util.UUID;

/**
 * A guess was made on a game that is already won or lost.
 */
public class GameOverException extends RuntimeException {

	public GameOverException(UUID gameId) {
		super("Game " + gameId + " is already finished");
	}
}
