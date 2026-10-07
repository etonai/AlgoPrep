package com.algoprep.studylist;

import com.algoprep.problem.ProblemRowText;

import java.time.LocalDate;
import java.util.Optional;

/** The text of one row in the study list tab. Pure, so it is testable without Swing. */
public final class StudyListRowText {

    public static final String NOT_FOUND = " - NOT FOUND";

    private StudyListRowText() {}

    /**
     * Found: {@code 1 - Two Sum, Easy, 20 minutes}, then {@code (selected)} and
     * {@code (STUDIED date)} as on the Problems tab. Not found: the key as written, the extras, and
     * {@code - NOT FOUND}, with no tags. A missing difficulty or time leaves no stray comma.
     */
    public static String of(StudyListRow row, boolean selected, Optional<LocalDate> studied) {
        String extras = row.entry().extrasText();
        if (row.problem().isEmpty()) {
            return row.entry().key() + extras + NOT_FOUND;
        }
        return ProblemRowText.of(row.problem().get().displayName() + extras, selected, studied);
    }
}
