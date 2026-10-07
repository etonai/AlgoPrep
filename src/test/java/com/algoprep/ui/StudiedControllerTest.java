package com.algoprep.ui;

import com.algoprep.config.SettingsStore;
import com.algoprep.problem.Problem;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Studied and Clear buttons on the MAIN tab and the Problems tab are bound to one controller.
 * These tests stand in two of each and check that they always behave identically.
 */
class StudiedControllerTest {

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private SettingsStore settings;
    private SelectedProblemModel selection;
    private StudiedStore store;
    private Problem problem;
    private Path home;
    private JButton studied1;
    private JButton studied2;
    private JButton clear1;
    private JButton clear2;
    private StudiedController controller;

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** Selection, settings and store changes reach the buttons through invokeLater. */
    private static void settle() throws Exception {
        edt(() -> { });
    }

    private final List<String> questions = new java.util.ArrayList<>();
    private boolean answer = true;

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        Path statement = Files.writeString(problems.resolve("0001_two-sum_problem.md"), "x");
        problem = new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.empty());
        home = Files.createDirectories(tmp.resolve("home"));

        settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setHomeDir(home.toString());
        selection = new SelectedProblemModel();

        store = new StudiedStore(Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC), status::add);
        store.setFile(StudiedStore.fileFor(settings.getHomeDir()).orElseThrow());

        StatusReporter reporter = new StatusReporter();
        reporter.attach(status::add);

        edt(() -> {
            controller = new StudiedController(settings, selection, store, reporter, (parent, message) -> {
                questions.add(message);
                return answer;
            });
            studied1 = new JButton("Studied");
            studied2 = new JButton("Studied");
            clear1 = new JButton("Clear Studied Tag");
            clear2 = new JButton("Clear Studied Tag");
            controller.bindStudied(studied1);
            controller.bindStudied(studied2);
            controller.bindClear(clear1);
            controller.bindClear(clear2);
        });
    }

    private void select() throws Exception {
        edt(() -> selection.select(problem));
        settle();
    }

    @Test
    void bothSetsOfButtonsFollowTheSameStateAndReason() throws Exception {
        assertFalse(studied1.isEnabled());
        assertFalse(studied2.isEnabled());
        assertEquals(studied1.getToolTipText(), studied2.getToolTipText());
        assertEquals("No problem selected.", studied1.getToolTipText());
        assertEquals(clear1.getToolTipText(), clear2.getToolTipText());

        select();

        assertTrue(studied1.isEnabled());
        assertTrue(studied2.isEnabled());
        assertEquals("Mark the selected problem as studied today", studied1.getToolTipText());
        assertFalse(clear1.isEnabled(), "nothing to clear yet");
        assertEquals("This problem is not marked as studied.", clear2.getToolTipText());
    }

    @Test
    void pressingEitherStudiedButtonRecordsTodayAndUpdatesBoth() throws Exception {
        select();

        edt(() -> studied2.doClick());
        settle();

        assertEquals(Optional.of(LocalDate.of(2026, 10, 7)), store.studiedOn(problem.key()));
        assertEquals(List.of("Marked 1 - Two Sum as studied on 2026-10-07."), status);
        assertTrue(clear1.isEnabled());
        assertTrue(clear2.isEnabled());
        assertEquals("Studied 2026-10-07. Press to update to today.", studied1.getToolTipText());
        assertEquals(studied1.getToolTipText(), studied2.getToolTipText());
    }

    @Test
    void clearRemovesTheDateFromEitherButton() throws Exception {
        select();
        edt(() -> studied1.doClick());
        settle();
        status.clear();

        edt(() -> clear2.doClick());
        settle();

        assertEquals(Optional.empty(), store.studiedOn(problem.key()));
        assertEquals(List.of("Cleared the studied tag for 1 - Two Sum."), status);
        assertEquals(List.of("Clear the studied tag for 1 - Two Sum?"), questions);
        assertFalse(clear1.isEnabled());
        assertFalse(clear2.isEnabled());
        assertEquals("Mark the selected problem as studied today", studied1.getToolTipText());
    }

    @Test
    void answeringNoKeepsTheDateAndSaysNothing() throws Exception {
        select();
        edt(() -> studied1.doClick());
        settle();
        status.clear();
        answer = false;

        edt(() -> clear1.doClick());
        settle();

        assertEquals(1, questions.size(), "it asked");
        assertEquals(Optional.of(LocalDate.of(2026, 10, 7)), store.studiedOn(problem.key()));
        assertEquals(List.of(), status);
        assertTrue(clear1.isEnabled());
    }

    @Test
    void noQuestionIsAskedWhenThereIsNothingToClear() throws Exception {
        select();
        edt(() -> clear1.doClick()); // disabled: doClick does nothing
        settle();

        assertEquals(List.of(), questions);
    }

    @Test
    void homeUnsetDisablesTheButtonsWithAReasonUntilItIsSet() throws Exception {
        select();
        edt(() -> settings.setHomeDir(null));
        settle();

        assertFalse(studied1.isEnabled());
        assertEquals("Select a HOME directory in Settings.", studied1.getToolTipText());
        assertEquals(studied1.getToolTipText(), studied2.getToolTipText());

        edt(() -> settings.setHomeDir(home.toString()));
        settle();
        assertTrue(studied1.isEnabled());
    }

    @Test
    void anUnavailableProblemCannotBeMarked() throws Exception {
        select();
        edt(() -> selection.reconcile(List.of())); // its statement has gone
        settle();

        assertFalse(studied1.isEnabled());
        assertFalse(studied2.isEnabled());
        assertTrue(studied1.getToolTipText().contains("Refresh"));
    }

    @Test
    void aFailedSaveReportsAnErrorAndNoSuccessMessage() throws Exception {
        select();
        Files.createDirectory(home.resolve(StudiedStore.FILE_NAME + ".tmp")); // makes the write fail

        edt(() -> studied1.doClick());
        settle();

        assertEquals(Optional.empty(), store.studiedOn(problem.key()));
        assertEquals(1, status.size());
        assertTrue(status.get(0).startsWith("Could not save"), status.get(0));
    }

    @Test
    void triggerDoesTheSameAsPressingTheButton() throws Exception {
        select();
        edt(() -> controller.trigger());

        assertTrue(store.studiedOn(problem.key()).isPresent());
    }

    @Test
    void triggerDoesNothingWhenDisabled() throws Exception {
        edt(() -> controller.trigger()); // nothing selected

        assertTrue(status.isEmpty());
        assertFalse(Files.exists(home.resolve(StudiedStore.FILE_NAME)));
    }
}
