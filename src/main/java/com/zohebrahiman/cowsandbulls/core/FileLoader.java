package com.zohebrahiman.cowsandbulls.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLoader {
	
	private static Logger log = LoggerFactory.getLogger(FileLoader.class);

	private static List<String> listOfSecrets = new ArrayList<String>();
	private static Random random = new Random();

	public static void loadFourLetters() throws IOException {
		InputStream is = FileLoader.class.getClass().getResourceAsStream("/four-letters.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String word;
		
		outer:
		while ((word = reader.readLine()) != null) {
			Set<Character> already = new HashSet<>();
			for (Character c : word.toCharArray()) {
				if (!already.add(c))
					continue outer;
			}
			listOfSecrets.add(word);
		}
		
		log.info("Added {} words from file", listOfSecrets.size());
		reader.close();
	}

	public static String getRandomSecretName() {
		int index = random.nextInt(listOfSecrets.size());
		return listOfSecrets.get(index);
	}

}
