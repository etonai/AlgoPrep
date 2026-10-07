package com.algoprep.display;

import com.algoprep.notes.HomeNotes;
import com.algoprep.problem.Problem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Decides what each of the three display tabs shows (Plan section 7): rendered Markdown text, or a
 * short message. Pure: no Swing, and no dependency on the settings class.
 *
 * <p>Only three files are ever opened: the statement, the supplied notes and the HOME notes. The
 * solution and test cases are never read.
 *
 * <p>Inputs are plain values. {@code current} is the selected problem while its statement is
 * found, and empty otherwise. {@code key} is the selected base key, present even when the problem
 * is unavailable. So "no key" means nothing selected, and "key but no problem" means the statement
 * has gone.
 */
public final class DocumentLoader {

    public static final long MAX_BYTES = 1024 * 1024;

    public static final String NOTHING_SELECTED = "No problem selected.";
    public static final String UNAVAILABLE =
            "This problem's statement is no longer available. Press Refresh on the Problems tab.";
    public static final String NO_SUPPLIED_NOTES = "No supplied notes for this problem.";
    public static final String NO_SAVED_NOTES = "No saved notes.";
    public static final String HOME_UNSET = "Select a HOME directory in Settings.";

    /** Either Markdown to render ({@code message} is null) or a message to show instead. */
    public record Doc(String markdown, String message) {
        public static Doc text(String markdown) { return new Doc(markdown, null); }
        public static Doc message(String message) { return new Doc(null, message); }
        public boolean isText() { return markdown != null; }
    }

    private DocumentLoader() {}

    public static Doc problem(Optional<Problem> current, Optional<String> key) {
        Optional<Doc> none = noProblem(current, key);
        if (none.isPresent()) {
            return none.get();
        }
        return readFile(current.get().statement());
    }

    public static Doc notes(Optional<Problem> current, Optional<String> key) {
        Optional<Doc> none = noProblem(current, key);
        if (none.isPresent()) {
            return none.get();
        }
        Optional<Path> notes = current.get().notes();
        // A notes file removed since the scan counts as missing, not as an error
        if (notes.isEmpty() || !Files.isRegularFile(notes.get())) {
            return Doc.message(NO_SUPPLIED_NOTES);
        }
        return readFile(notes.get());
    }

    /** HOME notes are found by key alone, so they still load when the statement has gone. */
    public static Doc myNotes(Optional<String> key, String homeDir) {
        if (key.isEmpty()) {
            return Doc.message(NOTHING_SELECTED);
        }
        if (homeDir == null || homeDir.isBlank()) {
            return Doc.message(HOME_UNSET);
        }
        Path home;
        try {
            home = Path.of(homeDir);
        } catch (RuntimeException e) {
            return Doc.message("HOME directory not found: " + homeDir);
        }
        if (!Files.isDirectory(home)) {
            return Doc.message("HOME directory not found: " + homeDir);
        }
        Optional<Path> file;
        try {
            file = HomeNotes.find(home, key.get());
        } catch (IOException e) {
            return Doc.message("Could not read the HOME directory: " + e.getMessage());
        }
        return file.map(DocumentLoader::readFile).orElseGet(() -> Doc.message(NO_SAVED_NOTES));
    }

    private static Optional<Doc> noProblem(Optional<Problem> current, Optional<String> key) {
        if (current.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(Doc.message(key.isPresent() ? UNAVAILABLE : NOTHING_SELECTED));
    }

    /** Reads UTF-8, replacing malformed bytes rather than failing, and drops a leading BOM. */
    static Doc readFile(Path file) {
        String name = file.getFileName().toString();
        try {
            long size = Files.size(file);
            if (size > MAX_BYTES) {
                return Doc.message(name + " is too large to display (" + describeSize(size) + ").");
            }
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) {
                text = text.substring(1);
            }
            if (text.isBlank()) {
                return Doc.message(name + " is empty.");
            }
            return Doc.text(text);
        } catch (NoSuchFileException e) {
            return Doc.message("Could not read " + name + ": file not found");
        } catch (IOException e) {
            return Doc.message("Could not read " + name + ": " + e.getMessage());
        }
    }

    static String describeSize(long bytes) {
        if (bytes < 1024 * 1024) {
            return Math.max(1, Math.round(bytes / 1024.0)) + " KB";
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
