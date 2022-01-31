package com.zohebrahiman.cowsandbulls;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.zohebrahiman.cowsandbulls.core.Calculator;
import com.zohebrahiman.cowsandbulls.core.WordHelper;
import com.zohebrahiman.cowsandbulls.model.GameRepository;
import com.zohebrahiman.cowsandbulls.model.Guess;
import com.zohebrahiman.cowsandbulls.model.GuessRepository;
import com.zohebrahiman.cowsandbulls.model.Secret;
import com.zohebrahiman.cowsandbulls.model.SecretRepository;

@Component
class Initializer implements CommandLineRunner {

	private final SecretRepository secretRepository;
	private final GuessRepository guessRepository;
	private final GameRepository gameRepository;

	public Initializer(SecretRepository secretRepository, GuessRepository guessRepository,
			GameRepository gameRepository) {
		this.secretRepository = secretRepository;
		this.guessRepository = guessRepository;
		this.gameRepository = gameRepository;
	}

	@Override
	public void run(String... strings) throws IOException, IllegalAccessException {
		// Load 4 letter words
		WordHelper.loadFourLetterSecretFile();

		Secret randomSecret = new Secret(WordHelper.getRandomSecret());
		secretRepository.save(randomSecret);
		secretRepository.findAll().forEach(System.out::println);

		Stream.of("abcd").forEach(name -> {
			int[] cowsAndBulls = Calculator.calculate(randomSecret.getName(), name);
			int cows = cowsAndBulls[0];
			int bulls = cowsAndBulls[1];
			Guess guess = Guess.builder().name(name).cows(cows).bulls(bulls).build();
			guessRepository.save(guess);
		});
		List<Guess> guesses = guessRepository.findAll();
		guesses.forEach(System.out::println);

//		Game game = Game.builder().secret(noteSecret).guesses(guesses).build();
//		gameRepository.save(game);
//        gameRepository.findAll().forEach(System.out::println);

	}
}
