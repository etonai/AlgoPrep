package com.algoprep.studied;

import com.algoprep.problem.Problem;
import com.algoprep.problem.ProblemRowText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudiedAvailabilityTest {

    @TempDir
    Path home;

    private static final Optional<LocalDate> NO_DATE = Optional.empty();
    private static final Optional<LocalDate> A_DATE = Optional.of(LocalDate.of(2026, 10, 3));

    private final Problem problem = new Problem("0001_two-sum", 1, "Two Sum",
            Path.of("0001_two-sum_problem.md"), Optional.empty());

    @Test
    void nothingSelectedBlocksBoth() {
        var r = StudiedAvailability.evaluate(Optional.empty(), Optional.empty(), home.toString(), NO_DATE);
        assertEquals(StudiedAvailability.NOTHING_SELECTED, r.markBlocker());
        assertEquals(StudiedAvailability.NOTHING_SELECTED, r.clearBlocker());
    }

    @Test
    void homeUnsetBlocksBoth() {
        var r = StudiedAvailability.evaluate(Optional.of(problem), Optional.of(problem.key()), null, NO_DATE);
        assertEquals(StudiedAvailability.HOME_UNSET, r.markBlocker());
        assertEquals(StudiedAvailability.HOME_UNSET, r.clearBlocker());

        r = StudiedAvailability.evaluate(Optional.of(problem), Optional.of(problem.key()), " ", NO_DATE);
        assertFalse(r.canMark());
    }

    @Test
    void homeMissingBlocksBothWithThePath() {
        String missing = home.resolve("nope").toString();
        var r = StudiedAvailability.evaluate(Optional.of(problem), Optional.of(problem.key()), missing, A_DATE);
        assertEquals("HOME directory not found: " + missing, r.markBlocker());
        assertFalse(r.canClear());
    }

    @Test
    void readySelectedProblemCanBeMarkedButClearNeedsADate() {
        var r = StudiedAvailability.evaluate(Optional.of(problem), Optional.of(problem.key()), home.toString(), NO_DATE);
        assertTrue(r.canMark());
        assertFalse(r.canClear());
        assertEquals(StudiedAvailability.NOT_STUDIED, r.clearBlocker());

        r = StudiedAvailability.evaluate(Optional.of(problem), Optional.of(problem.key()), home.toString(), A_DATE);
        assertTrue(r.canMark());
        assertTrue(r.canClear());
        assertEquals(A_DATE, r.date());
    }

    @Test
    void unavailableProblemCannotBeMarkedButItsDateCanBeCleared() {
        var r = StudiedAvailability.evaluate(Optional.empty(), Optional.of("0001_two-sum"), home.toString(), A_DATE);
        assertEquals(StudiedAvailability.UNAVAILABLE, r.markBlocker());
        assertTrue(r.canClear());
    }

    @Test
    void rowTextShowsSelectedAndStudiedTags() {
        assertEquals("1 - Two Sum", ProblemRowText.of("1 - Two Sum", false, NO_DATE));
        assertEquals("1 - Two Sum   (selected)", ProblemRowText.of("1 - Two Sum", true, NO_DATE));
        assertEquals("1 - Two Sum   (STUDIED 2026-10-03)", ProblemRowText.of("1 - Two Sum", false, A_DATE));
        assertEquals("1 - Two Sum   (selected)   (STUDIED 2026-10-03)",
                ProblemRowText.of("1 - Two Sum", true, A_DATE));
    }
}
