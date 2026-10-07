package com.algoprep.studylist;

/** The one-line summary under the study list, for example {@code 75 problems, 12 studied, 3 not found}. */
public final class StudyListSummary {

    private StudyListSummary() {}

    /**
     * @param total    rows in the list
     * @param shown    rows left after the filter
     * @param studied  rows (found) with a studied date
     * @param notFound rows with no files in the PROBLEMS directory
     */
    public static String text(int total, int shown, int studied, int notFound) {
        if (total == 0) {
            return "The study list is empty.";
        }
        StringBuilder text = new StringBuilder();
        if (shown != total) {
            text.append("Showing ").append(shown).append(" of ");
        }
        text.append(total).append(total == 1 ? " problem" : " problems");
        text.append(", ").append(studied).append(" studied");
        if (notFound > 0) {
            text.append(", ").append(notFound).append(" not found");
        }
        return text.toString();
    }
}
