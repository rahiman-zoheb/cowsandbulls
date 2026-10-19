package com.zohebrahiman.cowsandbulls.service;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {

	public GameNotFoundException(UUID gameId) {
		super("No game with id " + gameId);
	}
}
