package com.algoprep.problem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Scans a flat PROBLEMS directory using the filename convention (Plan section 5.1). Pure: no
 * Swing, runs on the calling thread.
 *
 * <p>A problem exists when a regular file ends in {@code _problem.md}; removing that suffix gives
 * the base key. Supplied notes are found only by the <em>full</em> base key
 * ({@code <key>_notes.md}), never by number or partial prefix. Names are matched case-insensitively.
 * Solutions, test cases and unrelated files are never listed or opened.
 */
public final class ProblemScanner {

    private static final String STATEMENT_SUFFIX = "_problem.md";
    private static final String NOTES_SUFFIX = "_notes.md";

    private ProblemScanner() {}

    public static List<Problem> scan(Path directory) throws IOException {
        return scan(directory, message -> System.err.println("[ProblemScanner] " + message));
    }

    /**
     * @param log receives a message for each name that looks like a statement but does not match
     *            the convention. These are logged, not shown to the user.
     * @throws java.nio.file.NoSuchFileException     if the directory does not exist
     * @throws java.nio.file.NotDirectoryException   if the path is not a directory
     * @throws IOException                           if the directory cannot be read
     */
    public static List<Problem> scan(Path directory, Consumer<String> log) throws IOException {
        // Real file names by lower-case name, so companions match case-insensitively
        Map<String, Path> byLowerName = new HashMap<>();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        }
        for (Path file : files) {
            byLowerName.put(file.getFileName().toString().toLowerCase(Locale.ROOT), file);
        }

        List<Problem> problems = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(STATEMENT_SUFFIX)) {
                if (lower.contains("problem")) {
                    log.accept("Ignoring '" + name + "': looks like a statement but does not end in "
                            + STATEMENT_SUFFIX);
                }
                continue;
            }
            String key = name.substring(0, name.length() - STATEMENT_SUFFIX.length());
            Optional<ProblemNames.KeyParts> parts = ProblemNames.parseKey(key);
            if (parts.isEmpty()) {
                log.accept("Ignoring '" + name + "': base key '" + key
                        + "' does not look like <number>_<name>");
                continue;
            }
            Optional<Path> notes = Optional.ofNullable(
                    byLowerName.get((key + NOTES_SUFFIX).toLowerCase(Locale.ROOT)));
            problems.add(new Problem(key, parts.get().number(),
                    ProblemNames.titleOf(parts.get().slug()), file, notes));
        }

        problems.sort(Comparator.comparingInt(Problem::number)
                .thenComparing(Problem::key, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Problem::key));
        return List.copyOf(problems);
    }
}
