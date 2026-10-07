package com.algoprep.studied;

import java.time.LocalDate;
import java.util.Optional;

/** The label at the top of the display window. Pure, so it is testable without Swing. */
public final class StudiedLabel {

    private StudiedLabel() {}

    /** {@code STUDIED 2026-10-07}, or empty when the problem has no studied date. */
    public static Optional<String> text(Optional<LocalDate> studied) {
        return studied.map(date -> "STUDIED " + date);
    }
}
