package com.zohebrahiman.cowsandbulls.core;

public class Calculator {

	public static int[] calculate(String secret, String guess) {
		int n = secret.length();

		if (guess.length() != secret.length()) {
			throw new RuntimeException("Guess length is not same as the secret length!");
		}

		int cows = 0;
		int bulls = 0;
		int[] counts = new int[26];
		for (int i = 0; i < n; i++) {
			int s = secret.charAt(i) - 'a';
			counts[s]++;
		}
		boolean[] visited = new boolean[n];
		for (int i = 0; i < n; i++) {
			int g = guess.charAt(i) - 'a';
			if (guess.charAt(i) == secret.charAt(i)) {
				bulls += 1;
				counts[g]--;
				visited[i] = true;

			}
		}
		for (int i = 0; i < n; i++) {
			if (visited[i])
				continue;
			int g = guess.charAt(i) - 'a';
			if (counts[g] > 0) {
				cows += 1;
				counts[g]--;
			}
		}
		return new int[] { cows, bulls };
	}

}
