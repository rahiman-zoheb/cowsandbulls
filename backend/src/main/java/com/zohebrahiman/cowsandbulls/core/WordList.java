package com.zohebrahiman.cowsandbulls.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The dictionary of four-letter words. Any of them is a legal guess; only those
 * with no repeated letter can be a secret. Loaded once at startup.
 */
@Component
public class WordList {

	private static final Logger log = LoggerFactory.getLogger(WordList.class);

	static final String RESOURCE = "/four-letters.txt";

	// Calculator indexes a 26-slot array with charAt(i) - 'a', so a secret
	// outside a-z would make every guess in that game throw.
	private static final Pattern FOUR_LETTERS = Pattern.compile("[a-z]{4}");

	private final Set<String> dictionary;
	private final List<String> secrets;

	// The secret is the one thing a player must not be able to work out, and
	// java.util.Random is predictable from its output.
	private final SecureRandom random = new SecureRandom();

	public WordList() throws IOException {
		this.dictionary = load();
		this.secrets = dictionary.stream().filter(WordList::isUsableSecret).sorted().toList();
		if (secrets.isEmpty()) {
			throw new IOException("No usable secrets in " + RESOURCE);
		}
		log.info("Loaded {} words, {} usable as secrets", dictionary.size(), secrets.size());
	}

	public boolean isWord(String word) {
		return dictionary.contains(word);
	}

	public String randomSecret() {
		return secrets.get(random.nextInt(secrets.size()));
	}

	List<String> secrets() {
		return secrets;
	}

	Set<String> dictionary() {
		return dictionary;
	}

	static boolean isUsableSecret(String word) {
		return FOUR_LETTERS.matcher(word).matches() && word.chars().distinct().count() == word.length();
	}

	private static Set<String> load() throws IOException {
		// Class.getResourceAsStream treats the leading slash as absolute;
		// ClassLoader.getResourceAsStream would not, and returns null.
		InputStream resource = WordList.class.getResourceAsStream(RESOURCE);
		if (resource == null) {
			throw new IOException("Missing word list on the classpath: " + RESOURCE);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
			return reader.lines()
					.map(String::trim)
					.filter(word -> FOUR_LETTERS.matcher(word).matches())
					.collect(Collectors.toUnmodifiableSet());
		}
	}
}
