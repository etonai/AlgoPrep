package com.algoprep.studied;

import com.algoprep.problem.Problem;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Decides whether the Studied and Clear buttons can be pressed, and why not. One decision shared by
 * every Studied button, so they cannot drift apart. Pure: no Swing.
 */
public final class StudiedAvailability {

    /**
     * @param markBlocker  why Studied is disabled, or null if it can be pressed
     * @param clearBlocker why Clear is disabled, or null if it can be pressed
     * @param date         the stored studied date for the selected problem, if any
     */
    public record Result(String markBlocker, String clearBlocker, Optional<LocalDate> date) {
        public boolean canMark()  { return markBlocker == null; }
        public boolean canClear() { return clearBlocker == null; }
    }

    public static final String NOTHING_SELECTED = "No problem selected.";
    public static final String UNAVAILABLE = "The selected problem's statement is gone. Refresh the Problems list.";
    public static final String HOME_UNSET = "Select a HOME directory in Settings.";
    public static final String NOT_STUDIED = "This problem is not marked as studied.";

    private StudiedAvailability() {}

    /**
     * @param current the selected problem while its statement is found, otherwise empty
     * @param key     the selected base key (present with no {@code current} means the statement has gone)
     * @param homeDir the HOME setting, where the studied file lives
     * @param date    the stored studied date for the selected key, if any
     */
    public static Result evaluate(Optional<Problem> current, Optional<String> key, String homeDir,
                                  Optional<LocalDate> date) {
        if (key.isEmpty()) {
            return new Result(NOTHING_SELECTED, NOTHING_SELECTED, Optional.empty());
        }
        String location = locationProblem(homeDir);
        if (location != null) {
            return new Result(location, location, date);
        }
        String mark = current.isEmpty() ? UNAVAILABLE : null;
        String clear = date.isEmpty() ? NOT_STUDIED : null;
        return new Result(mark, clear, date);
    }

    /** Why the studied file cannot be used, or null if its folder is fine. */
    private static String locationProblem(String homeDir) {
        if (homeDir == null || homeDir.isBlank()) {
            return HOME_UNSET;
        }
        try {
            if (!Files.isDirectory(Path.of(homeDir))) {
                return "HOME directory not found: " + homeDir;
            }
        } catch (InvalidPathException e) {
            return "HOME directory is not a valid path: " + homeDir;
        }
        return null;
    }
}
