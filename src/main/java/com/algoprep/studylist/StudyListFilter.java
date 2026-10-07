package com.algoprep.studylist;

import com.algoprep.problem.ProblemFilter;

import java.util.Locale;

/**
 * Matching for the study list tab's filter. A found row matches like the Problems tab (number or
 * name, zero-padded numbers too), and every row also matches its difficulty and time, so typing
 * {@code easy} shows only the easy ones. A row that was not found matches its key as written.
 */
public final class StudyListFilter {

    private StudyListFilter() {}

    public static boolean matches(StudyListRow row, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (row.entry().difficulty().toLowerCase(Locale.ROOT).contains(q)
                || row.entry().time().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (row.problem().isPresent()) {
            return ProblemFilter.matches(row.problem().get(), query);
        }
        return row.entry().key().toLowerCase(Locale.ROOT).contains(q);
    }
}
