package com.algoprep.upload;

import com.algoprep.problem.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UploadManifestTest {

    @TempDir
    Path tmp;

    private Path problems;
    private Path home;

    private Path write(Path dir, String name) throws IOException {
        return Files.writeString(dir.resolve(name), "x");
    }

    private void dirs() throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        home = Files.createDirectories(tmp.resolve("home"));
    }

    private Problem problem(Path statement, Path notes) {
        return new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.ofNullable(notes));
    }

    @Test
    void statementOnly() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");

        UploadManifest m = UploadManifest.build(problem(statement, null), null);

        assertTrue(m.ok());
        assertEquals(List.of(statement), m.files());
        assertEquals(List.of("0001_two-sum_problem.md"), m.names());
    }

    @Test
    void withSuppliedNotes() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notes = write(problems, "0001_two-sum_notes.md");

        UploadManifest m = UploadManifest.build(problem(statement, notes), null);

        assertEquals(List.of(statement, notes), m.files());
    }

    @Test
    void suppliedNotesDeletedSinceTheScanAreLeftOut() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notes = write(problems, "0001_two-sum_notes.md");
        Files.delete(notes);

        UploadManifest m = UploadManifest.build(problem(statement, notes), null);

        assertTrue(m.ok());
        assertEquals(List.of(statement), m.files());
    }

    @Test
    void withHomeNotes() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path mine = write(home, "0001_two-sum_AlgoPrepNotes.md");

        UploadManifest m = UploadManifest.build(problem(statement, null), home.toString());

        assertEquals(List.of(statement, mine), m.files());
    }

    @Test
    void allThreeInOrder() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notes = write(problems, "0001_two-sum_notes.md");
        Path mine = write(home, "0001_two-sum_AlgoPrepNotes.md");

        UploadManifest m = UploadManifest.build(problem(statement, notes), home.toString());

        assertEquals(List.of(statement, notes, mine), m.files());
        assertEquals(3, new HashSet<>(m.names()).size(), "names are distinct");
    }

    @Test
    void homeUnsetMissingOrWithoutNotesLeavesHomeNotesOut() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Problem p = problem(statement, null);

        assertEquals(List.of(statement), UploadManifest.build(p, null).files());
        assertEquals(List.of(statement), UploadManifest.build(p, "   ").files());
        assertEquals(List.of(statement), UploadManifest.build(p, tmp.resolve("missing").toString()).files());
        assertEquals(List.of(statement), UploadManifest.build(p, home.toString()).files());
        assertTrue(UploadManifest.build(p, tmp.resolve("missing").toString()).ok());
    }

    @Test
    void homeNotesForASimilarKeyAreNotIncluded() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        write(home, "0001_two-sum-ii_AlgoPrepNotes.md");

        assertEquals(List.of(statement), UploadManifest.build(problem(statement, null), home.toString()).files());
    }

    @Test
    void homeFileNameCaseIsIgnored() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path mine = write(home, "0001_TWO-SUM_algoprepnotes.MD");

        assertEquals(List.of(statement, mine),
                UploadManifest.build(problem(statement, null), home.toString()).files());
    }

    @Test
    void solutionTestCaseAndOtherFilesAreNeverIncluded() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        write(problems, "0001_two-sum_solution.java");
        write(problems, "0001_two-sum_testcases.md");
        write(problems, "0001_two-sum-ii_notes.md");
        write(home, "0001_two-sum_solution.java");
        write(home, "0001_two-sum_notes.md");

        UploadManifest m = UploadManifest.build(problem(statement, null), home.toString());

        assertEquals(List.of(statement), m.files());
        assertTrue(m.names().stream().noneMatch(n -> n.contains("solution") || n.contains("testcases")));
    }

    @Test
    void unreadableStatementIsAProblemNotASilentDrop() throws IOException {
        dirs();
        Path statement = problems.resolve("0001_two-sum_problem.md"); // never created

        UploadManifest m = UploadManifest.build(problem(statement, null), null);

        assertFalse(m.ok());
        assertTrue(m.problem().contains("0001_two-sum_problem.md"), m.problem());
        assertTrue(m.files().isEmpty());
    }

    @Test
    void existingNotesThatAreNotReadableFilesAreAProblem() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notesAsDirectory = Files.createDirectory(problems.resolve("0001_two-sum_notes.md"));

        UploadManifest m = UploadManifest.build(problem(statement, notesAsDirectory), null);

        assertFalse(m.ok());
        assertTrue(m.problem().contains("0001_two-sum_notes.md"), m.problem());
    }

    @Test
    void homeNotesThatAreNotReadableFilesAreAProblem() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Files.createDirectory(home.resolve("0001_two-sum_AlgoPrepNotes.md"));

        // A directory with that name is not a notes file, so HomeNotes ignores it: nothing to attach
        UploadManifest m = UploadManifest.build(problem(statement, null), home.toString());

        assertTrue(m.ok());
        assertEquals(List.of(statement), m.files());
    }

    @Test
    void orderIsStableAcrossCalls() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notes = write(problems, "0001_two-sum_notes.md");
        write(home, "0001_two-sum_AlgoPrepNotes.md");
        Problem p = problem(statement, notes);

        assertEquals(UploadManifest.build(p, home.toString()).names(),
                UploadManifest.build(p, home.toString()).names());
    }
}
