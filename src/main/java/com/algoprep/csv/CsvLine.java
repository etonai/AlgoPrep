package com.algoprep.csv;

import java.util.ArrayList;
import java.util.List;

/** Splits one line of a CSV file into fields. Pure: no file access, no Swing. */
public final class CsvLine {

    private CsvLine() {}

    /**
     * Splits a line at commas, honoring double quotes (a quoted field may contain commas, and
     * {@code ""} inside quotes is one quote). A quote in the middle of a field is kept as text.
     *
     * @return the fields, or null if a quote is never closed
     */
    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"' && current.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            return null;
        }
        fields.add(current.toString());
        return fields;
    }
}
