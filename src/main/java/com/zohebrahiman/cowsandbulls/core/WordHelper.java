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

public class WordHelper {

    private static Logger log = LoggerFactory.getLogger(WordHelper.class);

    private static List<String> listOfSecrets = new ArrayList<String>();
    private static Random random = new Random();

    public static void loadFourLetterSecretFile() throws IOException {
        String fileName = "/four-letters.txt";
        try (
                InputStream inputStream = new WordHelper().getClass().getClassLoader().getResourceAsStream(fileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            ) {
            String word;

            outer: while ((word = reader.readLine()) != null) {
                // Remove words with duplicate letter
                Set<Character> already = new HashSet<>();
                for (Character c : word.toCharArray()) {
                    if (!already.add(c))
                        continue outer;
                }
                // Add word to list
                listOfSecrets.add(word);
            }
            log.info("Added {} words from file", listOfSecrets.size());
        } catch (IOException e) {
            log.error("Error loading words", e);
            throw e;
        }
    }

    public static String getRandomSecret() throws IllegalAccessException {
        if (listOfSecrets.isEmpty()) {
            throw new IllegalAccessException("Secret list is not loaded");
        }
        
        int index = random.nextInt(listOfSecrets.size());
        return listOfSecrets.get(index);
    }

}
