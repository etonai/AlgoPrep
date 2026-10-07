package com.algoprep.problem;

import java.nio.file.Path;
import java.util.Optional;

/**
 * One problem found in the PROBLEMS directory.
 *
 * @param key       the base key, for example {@code 0001_two-sum}. This is the identity.
 * @param number    the leading number of the key
 * @param title     the formatted name without the number, for example {@code Two Sum}
 * @param statement the {@code <key>_problem.md} file (always present)
 * @param notes     the {@code <key>_notes.md} file, if it exists
 */
public record Problem(String key, int number, String title, Path statement, Optional<Path> notes) {

    /** The text shown in lists, for example {@code 1 - Two Sum}. */
    public String displayName() {
        return ProblemNames.display(number, title);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
