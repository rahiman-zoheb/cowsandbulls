package com.zohebrahiman.cowsandbulls.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The words a game's secret can be drawn from. Loaded once at startup.
 */
@Component
public class WordList {

	private static final Logger log = LoggerFactory.getLogger(WordList.class);

	static final String RESOURCE = "/four-letters.txt";

	// Calculator indexes a 26-slot array with charAt(i) - 'a', so a secret
	// outside a-z would make every guess in that game throw.
	private static final Pattern VALID_SECRET = Pattern.compile("[a-z]{4}");

	private final List<String> secrets;

	// The secret is the one thing a player must not be able to work out, and
	// java.util.Random is predictable from its output.
	private final SecureRandom random = new SecureRandom();

	public WordList() throws IOException {
		this.secrets = load();
		log.info("Loaded {} possible secrets", secrets.size());
	}

	public String randomSecret() {
		return secrets.get(random.nextInt(secrets.size()));
	}

	List<String> secrets() {
		return secrets;
	}

	static boolean isUsableSecret(String word) {
		return VALID_SECRET.matcher(word).matches() && word.chars().distinct().count() == word.length();
	}

	private static List<String> load() throws IOException {
		// Class.getResourceAsStream treats the leading slash as absolute;
		// ClassLoader.getResourceAsStream would not, and returns null.
		InputStream resource = WordList.class.getResourceAsStream(RESOURCE);
		if (resource == null) {
			throw new IOException("Missing word list on the classpath: " + RESOURCE);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			List<String> words = reader.lines().map(String::trim).filter(WordList::isUsableSecret).toList();
			if (words.isEmpty()) {
				throw new IOException("No usable secrets in " + RESOURCE);
			}
			return words;
		}
	}
}
