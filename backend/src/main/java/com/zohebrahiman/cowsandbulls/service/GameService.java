package com.zohebrahiman.cowsandbulls.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zohebrahiman.cowsandbulls.api.GameResponse;
import com.zohebrahiman.cowsandbulls.core.WordList;
import com.zohebrahiman.cowsandbulls.model.Game;
import com.zohebrahiman.cowsandbulls.model.GameRepository;

/**
 * Returns response DTOs rather than entities, so nothing that reaches a client
 * can carry the secret by accident. Mapping happens inside the transaction
 * because guesses are loaded lazily and open-in-view is off.
 */
@Service
@Transactional
public class GameService {

	private final GameRepository games;
	private final WordList words;

	public GameService(GameRepository games, WordList words) {
		this.games = games;
		this.words = words;
	}

	public GameResponse create() {
		return GameResponse.of(games.save(new Game(words.randomSecret())));
	}

	@Transactional(readOnly = true)
	public GameResponse get(UUID id) {
		return GameResponse.of(find(id));
	}

	public GameResponse guess(UUID id, String word) {
		Game game = find(id);
		game.guess(word);
		return GameResponse.of(game);
	}

	private Game find(UUID id) {
		return games.findById(id).orElseThrow(() -> new GameNotFoundException(id));
	}
}
