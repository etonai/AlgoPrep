package com.algoprep.problem;

import com.algoprep.config.SettingsStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProblemWorkspaceTest {

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private Path problems;
    private Path settingsFile;

    private void makeProblems(String... keys) throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        for (String k : keys) {
            Files.writeString(problems.resolve(k + "_problem.md"), "x");
        }
        settingsFile = tmp.resolve("settings.json");
    }

    private SettingsStore settings() {
        return new SettingsStore(settingsFile, status::add);
    }

    private ProblemWorkspace workspace() {
        return new ProblemWorkspace(settings(), status::add);
    }

    @Test
    void startsEmptyWhenNoDirectoryIsSaved() throws IOException {
        makeProblems("0001_a");
        ProblemWorkspace w = workspace();
        assertTrue(w.catalog().problems().isEmpty());
        assertTrue(w.selection().selectedKey().isEmpty());
        assertNull(w.directoryText());
    }

    @Test
    void settingTheDirectoryScansSavesAndReports() throws IOException {
        makeProblems("0001_a", "0002_b");
        ProblemWorkspace w = workspace();

        w.setDirectory(problems);

        assertEquals(2, w.catalog().problems().size());
        assertEquals(problems.toString(), w.directoryText());
        assertEquals(problems.toString(), settings().getProblemsDir());
        assertEquals("2 problem(s) found", status.get(status.size() - 1));
    }

    @Test
    void selectionIsSavedAndRestoredAtStartupWhenFound() throws IOException {
        makeProblems("0001_a", "0002_b");
        ProblemWorkspace first = workspace();
        first.setDirectory(problems);
        first.selection().select(first.catalog().problems().get(1));
        assertEquals("0002_b", settings().getLastSelectedKey());

        ProblemWorkspace second = workspace();

        assertEquals("0002_b", second.selection().current().orElseThrow().key());
    }

    @Test
    void savedKeyThatIsNoLongerFoundIsNotRestored() throws IOException {
        makeProblems("0001_a", "0002_b");
        ProblemWorkspace first = workspace();
        first.setDirectory(problems);
        first.selection().select(first.catalog().problems().get(1));
        Files.delete(problems.resolve("0002_b_problem.md"));

        ProblemWorkspace second = workspace();

        assertTrue(second.selection().selectedKey().isEmpty());
        assertFalse(second.selection().isUnavailable());
        assertEquals(1, second.catalog().problems().size());
    }

    @Test
    void changingTheDirectoryClearsTheSelection() throws IOException {
        makeProblems("0001_a");
        ProblemWorkspace w = workspace();
        w.setDirectory(problems);
        w.selection().select(w.catalog().problems().get(0));
        Path other = Files.createDirectories(tmp.resolve("other"));
        Files.writeString(other.resolve("0001_a_problem.md"), "x");

        w.setDirectory(other);

        assertTrue(w.selection().selectedKey().isEmpty(), "same key in another directory is still cleared");
        assertNull(settings().getLastSelectedKey());
    }

    @Test
    void choosingTheSameDirectoryAgainOnlyRescans() throws IOException {
        makeProblems("0001_a");
        ProblemWorkspace w = workspace();
        w.setDirectory(problems);
        w.selection().select(w.catalog().problems().get(0));
        Files.writeString(problems.resolve("0002_b_problem.md"), "x");

        w.setDirectory(problems);

        assertEquals(2, w.catalog().problems().size());
        assertEquals("0001_a", w.selection().selectedKey().orElseThrow());
    }

    @Test
    void refreshWithStatementRemovedMarksSelectionUnavailable() throws IOException {
        makeProblems("0001_a", "0002_b");
        ProblemWorkspace w = workspace();
        w.setDirectory(problems);
        w.selection().select(w.catalog().problems().get(0));
        Files.delete(problems.resolve("0001_a_problem.md"));

        w.refresh();

        assertTrue(w.selection().isUnavailable());
        assertTrue(w.selection().current().isEmpty());
        assertEquals("0001_a", w.selection().selectedKey().orElseThrow());
    }

    @Test
    void savedDirectoryThatNoLongerExistsIsKeptAndReported() throws IOException {
        makeProblems("0001_a");
        ProblemWorkspace first = workspace();
        first.setDirectory(problems);
        first.selection().select(first.catalog().problems().get(0));
        Files.delete(problems.resolve("0001_a_problem.md"));
        Files.delete(problems);
        status.clear();

        ProblemWorkspace second = workspace();

        assertEquals(problems.toString(), second.directoryText(), "path stays visible");
        assertTrue(second.catalog().problems().isEmpty());
        assertTrue(status.stream().anyMatch(s -> s.contains("not found")));
        assertEquals("0001_a", settings().getLastSelectedKey(), "saved key is not thrown away");
    }

    @Test
    void refreshWithNoDirectoryReportsHowToSetOne() throws IOException {
        makeProblems();
        ProblemWorkspace w = workspace();

        w.refresh();

        assertTrue(status.get(status.size() - 1).contains("No PROBLEMS directory set"));
    }
}
