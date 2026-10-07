package com.algoprep.problem;

import java.util.Locale;

/**
 * Matching for the Problems tab filter. It matches what the user sees (the number and name), plus
 * zero-padded numbers, so {@code 1} and {@code 0001} both find problem 1. It does not match the
 * base key or file names.
 */
public final class ProblemFilter {

    private ProblemFilter() {}

    public static boolean matches(Problem problem, String query) {
        if (query == null) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return true;
        }
        if (problem.displayName().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (q.chars().allMatch(Character::isDigit)) {
            String unpadded = q.replaceFirst("^0+", "");
            if (unpadded.isEmpty()) {
                unpadded = "0";
            }
            return String.valueOf(problem.number()).contains(unpadded);
        }
        return false;
    }
}
