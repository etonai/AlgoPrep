package com.algoprep.notes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the user's saved personal notes, {@code <key>_AlgoPrepNotes.md}, in the HOME directory
 * (Plan sections 5.1 and 11). Read only: AlgoPrep never writes these files.
 *
 * <p>The directory is listed and the full name matched case-insensitively, like
 * {@code ProblemScanner}, so the match does not depend on the file system's case rules. Only the
 * <em>full</em> base key matches, so notes for {@code 0001_two-sum-ii} are never shown for
 * {@code 0001_two-sum}.
 */
public final class HomeNotes {

    public static final String SUFFIX = "_AlgoPrepNotes.md";

    private HomeNotes() {}

    /** The file name for a base key, for example {@code 0001_two-sum_AlgoPrepNotes.md}. */
    public static String fileName(String key) {
        return key + SUFFIX;
    }

    /**
     * @return the real path of the notes file, or empty if there is none
     * @throws IOException if the HOME directory cannot be listed
     */
    public static Optional<Path> find(Path homeDir, String key) throws IOException {
        String wanted = fileName(key).toLowerCase(Locale.ROOT);
        try (Stream<Path> stream = Files.list(homeDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).equals(wanted))
                    .filter(Files::isRegularFile)
                    .findFirst();
        }
    }
}
