package com.algoprep.problem;

import java.time.LocalDate;
import java.util.Optional;

/** The text of one row in the Problems list. Pure, so it is testable without Swing. */
public final class ProblemRowText {

    private ProblemRowText() {}

    /**
     * {@code 1 - Two Sum}, then {@code (selected)} if it is the selected problem, then
     * {@code (STUDIED 2026-10-07)} if it has a studied date.
     */
    public static String of(String displayName, boolean selected, Optional<LocalDate> studied) {
        StringBuilder text = new StringBuilder(displayName);
        if (selected) {
            text.append("   (selected)");
        }
        studied.ifPresent(date -> text.append("   (STUDIED ").append(date).append(')'));
        return text.toString();
    }
}
