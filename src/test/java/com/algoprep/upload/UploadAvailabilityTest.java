package com.algoprep.upload;

import com.algoprep.problem.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UploadAvailabilityTest {

    @TempDir
    Path tmp;

    private Path problems;
    private Path home;
    private Path root;

    private Problem problem() throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        home = Files.createDirectories(tmp.resolve("home"));
        root = tmp.resolve("staging");
        Path statement = Files.writeString(problems.resolve("0001_two-sum_problem.md"), "x");
        Path notes = Files.writeString(problems.resolve("0001_two-sum_notes.md"), "x");
        return new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.of(notes));
    }

    private UploadAvailability.Result evaluate(Optional<Problem> p, Optional<String> key,
                                               String browserProblem, boolean cooling) {
        return UploadAvailability.evaluate(p, key, home == null ? null : home.toString(),
                problems == null ? null : problems.toString(), root, browserProblem, cooling);
    }

    @Test
    void canUploadAndListsTheExactFiles() throws IOException {
        Problem p = problem();

        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()), null, false);

        assertTrue(r.canUpload());
        assertNull(r.blocker());
        assertEquals("0001_two-sum_problem.md, 0001_two-sum_notes.md", r.summary());
    }

    @Test
    void nothingSelected() throws IOException {
        problem();

        UploadAvailability.Result r = evaluate(Optional.empty(), Optional.empty(), null, false);

        assertFalse(r.canUpload());
        assertEquals(UploadService.NOTHING_SELECTED, r.blocker());
        assertEquals(UploadService.NOTHING_SELECTED, r.summary());
    }

    @Test
    void unavailableProblem() throws IOException {
        problem();

        UploadAvailability.Result r = evaluate(Optional.empty(), Optional.of("0001_two-sum"), null, false);

        assertEquals(UploadService.UNAVAILABLE, r.blocker());
    }

    @Test
    void unreadableFileBlocksAndIsExplainedInTheSummary() throws IOException {
        Problem p = problem();
        Files.delete(p.statement());

        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()), null, false);

        assertFalse(r.canUpload());
        assertTrue(r.blocker().contains("0001_two-sum_problem.md"), r.blocker());
        assertEquals(r.blocker(), r.summary());
    }

    @Test
    void overlappingStagingRootBlocksButStillShowsTheFiles() throws IOException {
        Problem p = problem();
        root = problems; // staging into PROBLEMS must never be allowed

        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()), null, false);

        assertFalse(r.canUpload());
        assertTrue(r.blocker().contains("Fix it in Settings."), r.blocker());
        assertEquals("0001_two-sum_problem.md, 0001_two-sum_notes.md", r.summary());
    }

    @Test
    void unresolvedStagingRootBlocks() throws IOException {
        Problem p = problem();
        root = null;

        assertFalse(evaluate(Optional.of(p), Optional.of(p.key()), null, false).canUpload());
    }

    @Test
    void browserNotReadyBlocksWithItsOwnReason() throws IOException {
        Problem p = problem();

        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()),
                "ChatGPT is not ready (Sending)", false);

        assertEquals("ChatGPT is not ready (Sending)", r.blocker());
        assertEquals("0001_two-sum_problem.md, 0001_two-sum_notes.md", r.summary());
    }

    @Test
    void coolingDownBlocks() throws IOException {
        Problem p = problem();

        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()), null, true);

        assertEquals(UploadAvailability.COOLING_DOWN, r.blocker());
    }

    @Test
    void problemsAreReportedInOrderOfImportance() throws IOException {
        Problem p = problem();
        root = problems;

        // Overlap is reported before the browser and the cooldown
        UploadAvailability.Result r = evaluate(Optional.of(p), Optional.of(p.key()), "ChatGPT is not ready", true);
        assertTrue(r.blocker().contains("Fix it in Settings."));

        root = tmp.resolve("staging");
        r = evaluate(Optional.of(p), Optional.of(p.key()), "ChatGPT is not ready", true);
        assertEquals("ChatGPT is not ready", r.blocker());
    }
}
