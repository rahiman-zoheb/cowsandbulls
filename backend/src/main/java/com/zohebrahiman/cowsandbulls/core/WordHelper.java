package com.zohebrahiman.cowsandbulls.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        // ClassLoader.getResourceAsStream does not strip a leading slash, so the
        // previous lookup returned null and NPEd on any plain classpath -- which
        // is why the only test in the repo could never load the context. Class
        // .getResourceAsStream does treat a leading slash as absolute.
        InputStream resource = WordHelper.class.getResourceAsStream(fileName);
        if (resource == null) {
            throw new IOException("Missing word list on the classpath: " + fileName);
        }
        try (
                InputStream inputStream = resource;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            ) {
            // Reloading in the same JVM (e.g. a second Spring context in a
            // test run) would otherwise append a duplicate copy of the list.
            listOfSecrets.clear();

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
