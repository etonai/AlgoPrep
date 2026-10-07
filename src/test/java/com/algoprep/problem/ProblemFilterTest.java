package com.algoprep.problem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProblemFilterTest {

    private static Problem p(int number, String title) {
        return new Problem(String.format("%04d_x", number), number, title, Path.of("s.md"), Optional.empty());
    }

    @Test
    void emptyOrNullQueryMatchesEverything() {
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), ""));
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), "   "));
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), null));
    }

    @Test
    void matchesNameCaseInsensitivelyAsSubstring() {
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), "two"));
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), "SUM"));
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), "o su"));
        assertFalse(ProblemFilter.matches(p(1, "Two Sum"), "three"));
    }

    @Test
    void paddedAndUnpaddedNumbersBothMatch() {
        Problem one = p(1, "Two Sum");
        assertTrue(ProblemFilter.matches(one, "1"));
        assertTrue(ProblemFilter.matches(one, "0001"));
        assertTrue(ProblemFilter.matches(one, "01"));
        assertFalse(ProblemFilter.matches(one, "2"));
        assertFalse(ProblemFilter.matches(one, "0002"));
    }

    @Test
    void zeroQueryMatchesNumberZeroOnly() {
        assertTrue(ProblemFilter.matches(p(0, "X"), "0"));
        assertTrue(ProblemFilter.matches(p(0, "X"), "000"));
        assertFalse(ProblemFilter.matches(p(5, "X"), "000"));
    }

    @Test
    void displayedTextWithSeparatorMatches() {
        assertTrue(ProblemFilter.matches(p(1, "Two Sum"), "1 - two"));
    }

    @Test
    void doesNotMatchTheBaseKeyOrFileNames() {
        Problem p = new Problem("0001_two-sum", 1, "Two Sum", Path.of("0001_two-sum_problem.md"), Optional.empty());
        assertFalse(ProblemFilter.matches(p, "two-sum"));
        assertFalse(ProblemFilter.matches(p, "_problem"));
    }
}
