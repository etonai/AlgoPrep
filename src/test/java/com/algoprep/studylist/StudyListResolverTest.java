package com.algoprep.studylist;

import com.algoprep.problem.Problem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudyListResolverTest {

    private static Problem problem(String key, int number, String title) {
        return new Problem(key, number, title, Path.of(key + "_problem.md"), Optional.empty());
    }

    private static final List<Problem> CATALOG = List.of(
            problem("0001_two-sum", 1, "Two Sum"),
            problem("0099_nodifficulty", 99, "Nodifficulty"),
            problem("0105_notime", 105, "Notime"));

    private static final Optional<LocalDate> NOT_STUDIED = Optional.empty();

    private static StudyListEntry e(String key, String difficulty, String time) {
        return new StudyListEntry(key, difficulty, time);
    }

    private static List<StudyListRow> idea() {
        return StudyListResolver.resolve(List.of(
                e("0001_two-sum", "Easy", "20 minutes"),
                e("15_not-found", "Tough", "15 minutes"),
                e("99_nodifficulty", "", "2 minutes"),
                e("105_notime", "Medium", "")), CATALOG);
    }

    @Test
    void theIdeasExampleProducesExactlyTheFourSampleRows() {
        List<StudyListRow> rows = idea();
        List<String> texts = rows.stream().map(r -> StudyListRowText.of(r, false, NOT_STUDIED)).toList();

        assertEquals(List.of(
                "1 - Two Sum, Easy, 20 minutes",
                "15_not-found, Tough, 15 minutes - NOT FOUND",
                "99 - Nodifficulty, 2 minutes",
                "105 - Notime, Medium"), texts);
        assertTrue(rows.get(0).isFound());
        assertFalse(rows.get(1).isFound());
    }

    @Test
    void zeroPaddingDifferencesMatchInBothDirections() {
        var rows = StudyListResolver.resolve(List.of(e("1_two-sum", "", ""), e("0099_nodifficulty", "", "")), CATALOG);
        assertTrue(rows.get(0).isFound());
        assertEquals("0001_two-sum", rows.get(0).problem().get().key());
        assertTrue(rows.get(1).isFound());
    }

    @Test
    void theSlugMatchesCaseInsensitively() {
        var rows = StudyListResolver.resolve(List.of(e("0001_TWO-Sum", "", "")), CATALOG);
        assertTrue(rows.get(0).isFound());
    }

    @Test
    void similarNamesNeverMatch() {
        var rows = StudyListResolver.resolve(List.of(
                e("0001_two-sum-ii", "", ""),   // longer slug
                e("0001_two", "", ""),          // partial slug
                e("0002_two-sum", "", "")),     // same slug, other number
                CATALOG);
        assertTrue(rows.stream().noneMatch(StudyListRow::isFound));
    }

    @Test
    void aKeyThatIsNotNumberAndSlugIsNotFoundAndKeepsItsText() {
        var rows = StudyListResolver.resolve(List.of(e("two-sum", "Easy", "")), CATALOG);
        assertFalse(rows.get(0).isFound());
        assertEquals("two-sum, Easy - NOT FOUND", StudyListRowText.of(rows.get(0), false, NOT_STUDIED));
    }

    @Test
    void theListOrderIsKeptNotSortedByNumber() {
        var rows = StudyListResolver.resolve(List.of(
                e("0105_notime", "", ""), e("0001_two-sum", "", ""), e("0099_nodifficulty", "", "")), CATALOG);
        assertEquals(List.of("0105_notime", "0001_two-sum", "0099_nodifficulty"),
                rows.stream().map(r -> r.entry().key()).toList());
    }

    @Test
    void duplicateEntriesAreBothKept() {
        var rows = StudyListResolver.resolve(List.of(e("0001_two-sum", "", ""), e("0001_two-sum", "", "")), CATALOG);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(StudyListRow::isFound));
    }

    @Test
    void emptyInputsGiveEmptyOrNotFoundRows() {
        assertTrue(StudyListResolver.resolve(List.of(), CATALOG).isEmpty());
        var rows = StudyListResolver.resolve(List.of(e("0001_two-sum", "", "")), List.of());
        assertFalse(rows.get(0).isFound());
    }

    // ---- row text tags ----

    @Test
    void foundRowsCarrySelectedAndStudiedTagsLikeTheProblemsTab() {
        StudyListRow row = idea().get(0);
        Optional<LocalDate> date = Optional.of(LocalDate.of(2026, 10, 7));

        assertEquals("1 - Two Sum, Easy, 20 minutes   (selected)", StudyListRowText.of(row, true, NOT_STUDIED));
        assertEquals("1 - Two Sum, Easy, 20 minutes   (STUDIED 2026-10-07)", StudyListRowText.of(row, false, date));
        assertEquals("1 - Two Sum, Easy, 20 minutes   (selected)   (STUDIED 2026-10-07)",
                StudyListRowText.of(row, true, date));
    }

    @Test
    void notFoundRowsNeverShowSelectedOrStudiedTags() {
        StudyListRow row = idea().get(1);
        assertEquals("15_not-found, Tough, 15 minutes - NOT FOUND",
                StudyListRowText.of(row, true, Optional.of(LocalDate.of(2026, 10, 7))));
    }

    @Test
    void blankDifficultyAndTimeLeaveNoStrayCommas() {
        var rows = StudyListResolver.resolve(List.of(e("0001_two-sum", "", "")), CATALOG);
        assertEquals("1 - Two Sum", StudyListRowText.of(rows.get(0), false, NOT_STUDIED));
    }

    // ---- filter and summary ----

    @Test
    void filterMatchesNumberNameDifficultyAndTime() {
        List<StudyListRow> rows = idea();
        assertTrue(StudyListFilter.matches(rows.get(0), ""));
        assertTrue(StudyListFilter.matches(rows.get(0), "two"));
        assertTrue(StudyListFilter.matches(rows.get(0), "0001"), "zero-padded number");
        assertTrue(StudyListFilter.matches(rows.get(0), "EASY"));
        assertTrue(StudyListFilter.matches(rows.get(0), "20 min"));
        assertFalse(StudyListFilter.matches(rows.get(0), "hard"));
        assertTrue(StudyListFilter.matches(rows.get(1), "not-found"), "not found rows match their key");
        assertTrue(StudyListFilter.matches(rows.get(1), "tough"));
        assertFalse(StudyListFilter.matches(rows.get(1), "two"));
    }

    @Test
    void summaryTextShowsCountsAndOmitsNotFoundWhenZero() {
        assertEquals("75 problems, 12 studied, 3 not found", StudyListSummary.text(75, 75, 12, 3));
        assertEquals("75 problems, 0 studied", StudyListSummary.text(75, 75, 0, 0));
        assertEquals("1 problem, 1 studied", StudyListSummary.text(1, 1, 1, 0));
        assertEquals("Showing 4 of 75 problems, 12 studied, 3 not found", StudyListSummary.text(75, 4, 12, 3));
        assertEquals("The study list is empty.", StudyListSummary.text(0, 0, 0, 0));
    }
}
