package com.algoprep.studied;

import com.algoprep.problem.ProblemNames;

import java.util.Optional;

/** The text offered for the clipboard after a problem is marked studied. No Swing dependency. */
public final class StudiedPrompt {

    private StudiedPrompt() {}

    /**
     * {@code Studied leetcode problem #1} for {@code 0001_two-sum}: the number without zero padding.
     * Empty when the key has no number.
     */
    public static Optional<String> forKey(String key) {
        return ProblemNames.parseKey(key).map(p -> "Studied leetcode problem #" + p.number());
    }
}
