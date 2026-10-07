package com.algoprep.studylist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads a study list file. Never throws for a bad file: the result carries an error message instead.
 * AlgoPrep only reads these files, never writes them.
 */
public final class StudyListFile {

    /** @param error a message if the file could not be read, otherwise null */
    public record Result(List<StudyListEntry> entries, int skipped, String error) {
        public boolean ok() { return error == null; }
    }

    private StudyListFile() {}

    public static Result load(Path file) {
        if (!Files.exists(file)) {
            return failed("Study list file not found: " + file);
        }
        if (Files.isDirectory(file)) {
            return failed("Study list path is a folder, not a file: " + file);
        }
        try {
            // Malformed bytes become replacement characters instead of failing the whole load
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            StudyListParser.Parsed parsed = StudyListParser.parse(text);
            return new Result(parsed.entries(), parsed.skipped(), null);
        } catch (IOException e) {
            return failed("Could not read the study list " + file.getFileName() + ": " + e.getMessage());
        }
    }

    private static Result failed(String message) {
        return new Result(List.of(), 0, message);
    }
}
