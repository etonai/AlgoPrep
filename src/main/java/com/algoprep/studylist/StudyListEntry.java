package com.algoprep.studylist;

/**
 * One line of a study list file: the problem key as written, and the optional difficulty and time.
 * The last two are empty strings when missing.
 */
public record StudyListEntry(String key, String difficulty, String time) {

    /** {@code ", Easy, 20 minutes"}, {@code ", Medium"}, {@code ", 2 minutes"} or empty. */
    public String extrasText() {
        StringBuilder text = new StringBuilder();
        if (!difficulty.isEmpty()) {
            text.append(", ").append(difficulty);
        }
        if (!time.isEmpty()) {
            text.append(", ").append(time);
        }
        return text.toString();
    }
}
