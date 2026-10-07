package com.algoprep.studylist;

import com.algoprep.csv.CsvLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the text of a study list file into entries, in file order. Pure: no file access, no Swing.
 *
 * <p>One problem per line: {@code key, difficulty, time}. Spaces around the commas are ignored. A
 * line with fewer fields leaves the rest blank. Blank lines and lines starting with {@code #} are
 * ignored, fields beyond the third are ignored, and a line with a blank key or an unclosed quote is
 * skipped and counted. The file may start with a header row such as {@code problem,difficulty,time}
 * (or {@code key,difficulty,time}), which is skipped and not counted.
 */
public final class StudyListParser {

    /** @param skipped how many non-blank, non-comment lines could not be used */
    public record Parsed(List<StudyListEntry> entries, int skipped) { }

    private StudyListParser() {}

    public static Parsed parse(String text) {
        List<StudyListEntry> entries = new ArrayList<>();
        int skipped = 0;
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        boolean first = true; // a header row can only be the first line with content
        for (String line : text.split("\r?\n", -1)) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }
            List<String> fields = CsvLine.parse(line);
            boolean mayBeHeader = first;
            first = false;
            if (mayBeHeader && isHeader(fields)) {
                continue;
            }
            if (fields == null || fields.get(0).isBlank()) {
                skipped++;
                continue;
            }
            entries.add(new StudyListEntry(
                    fields.get(0).trim(),
                    fields.size() > 1 ? fields.get(1).trim() : "",
                    fields.size() > 2 ? fields.get(2).trim() : ""));
        }
        return new Parsed(entries, skipped);
    }

    /**
     * A header row names the columns: {@code problem} (or {@code key}) first, then {@code difficulty}
     * and {@code time} if present. Real keys look like {@code 0001_two-sum}, so a first field of
     * just {@code problem} or {@code key} is never a problem.
     */
    private static boolean isHeader(List<String> fields) {
        if (fields == null) {
            return false;
        }
        String name = fields.get(0).trim();
        if (!name.equalsIgnoreCase("problem") && !name.equalsIgnoreCase("key")) {
            return false;
        }
        String difficulty = fields.size() > 1 ? fields.get(1).trim() : "";
        return difficulty.isEmpty() || difficulty.equalsIgnoreCase("difficulty");
    }
}
