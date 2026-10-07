package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.Problem;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.upload.UploadAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Upload buttons on the MAIN tab and the Problems tab are bound to one controller. These tests
 * stand in two buttons and check that they always behave identically.
 */
class UploadControllerTest {

    private static final int COOLDOWN_MS = 150;

    @TempDir
    Path tmp;

    private final List<String> calls = new ArrayList<>();
    private final List<String> status = new ArrayList<>();
    private AppState appState;
    private SettingsStore settings;
    private SelectedProblemModel selection;
    private Problem problem;
    private UploadController controller;
    private JButton first;
    private JButton second;

    private final ChatBridge bridge = new ChatBridge() {
        @Override public void sendPrompt(String prompt, ResponseListener l) { }
        @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
        @Override public void reset() { calls.add("reset"); }
        @Override public void clickUploadFile() { calls.add("click"); }
    };

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        Path statement = Files.writeString(problems.resolve("0001_two-sum_problem.md"), "x");
        problem = new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.empty());

        appState = new AppState();
        appState.browserLoadStarted("https://chatgpt.com");
        appState.browserLoadFinished("https://chatgpt.com", true);

        settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setStagingRoot(tmp.resolve("stage").toString());
        selection = new SelectedProblemModel();

        StatusReporter reporter = new StatusReporter();
        reporter.attach(status::add);

        edt(() -> {
            controller = new UploadController(appState, settings, bridge, reporter,
                    selection, tmp.resolve("default-stage").toString(), COOLDOWN_MS);
            first = new JButton("Upload");
            second = new JButton("Upload");
            controller.bind(first);
            controller.bind(second);
        });
    }

    private void select() throws Exception {
        edt(() -> selection.select(problem));
        edt(() -> { }); // selection changes are applied through invokeLater
    }

    @Test
    void bothButtonsFollowTheSameStateAndReason() throws Exception {
        // Nothing selected
        assertFalse(first.isEnabled());
        assertFalse(second.isEnabled());
        assertEquals(first.getToolTipText(), second.getToolTipText());
        assertEquals("No problem selected.", first.getToolTipText());

        select();

        assertTrue(first.isEnabled());
        assertTrue(second.isEnabled());
        assertEquals(first.getToolTipText(), second.getToolTipText());
    }

    @Test
    void pressingEitherButtonUploadsOnceAndDisablesBoth() throws Exception {
        select();

        edt(() -> second.doClick());

        assertEquals(List.of("reset", "click"), calls);
        assertFalse(first.isEnabled(), "cooldown disables the other button too");
        assertFalse(second.isEnabled());
        assertEquals(UploadAvailability.COOLING_DOWN, first.getToolTipText());
        assertEquals(List.of("1 file(s) staged. Select them in the ChatGPT file dialog."), status);
    }

    @Test
    void aSecondPressDuringTheCooldownDoesNothing() throws Exception {
        select();

        edt(() -> {
            first.doClick();
            second.doClick();
            first.doClick();
        });

        assertEquals(List.of("reset", "click"), calls, "only one dialog opens");
    }

    @Test
    void buttonsComeBackAfterTheCooldown() throws Exception {
        select();
        edt(() -> first.doClick());

        Thread.sleep(COOLDOWN_MS * 4);
        edt(() -> { });

        assertTrue(first.isEnabled());
        assertTrue(second.isEnabled());
    }

    @Test
    void lastAttachedIsSharedAndClearedWhenThePageReloads() throws Exception {
        select();
        assertTrue(controller.lastAttachedName().isEmpty());

        edt(() -> second.doClick());
        assertEquals("1 - Two Sum", controller.lastAttachedName().orElseThrow());

        edt(() -> appState.browserLoadStarted("https://chatgpt.com"));
        assertTrue(controller.lastAttachedName().isEmpty());
    }

    @Test
    void bothButtonsAreDisabledWhileTheBrowserIsNotReady() throws Exception {
        select();

        edt(() -> appState.browserLoadStarted("https://chatgpt.com"));

        assertFalse(first.isEnabled());
        assertFalse(second.isEnabled());
        assertTrue(first.getToolTipText().contains("not ready"), first.getToolTipText());
        assertEquals(first.getToolTipText(), second.getToolTipText());
    }

    @Test
    void bothButtonsAreDisabledWhenTheStagingRootOverlapsProblems() throws Exception {
        select();
        Path problems = problem.statement().getParent();

        edt(() -> {
            settings.setProblemsDir(problems.toString());
            settings.setStagingRoot(problems.toString());
        });
        edt(() -> { });

        assertFalse(first.isEnabled());
        assertFalse(second.isEnabled());
        assertTrue(first.getToolTipText().contains("Fix it in Settings."), first.getToolTipText());
        assertTrue(Files.exists(problem.statement()), "nothing was deleted");
    }

    @Test
    void triggerUploadsWhenAllowedAndDoesNothingWhenBlocked() throws Exception {
        edt(() -> controller.trigger());
        assertTrue(calls.isEmpty(), "blocked: nothing selected");

        select();
        edt(() -> controller.trigger());

        assertEquals(List.of("reset", "click"), calls);
    }

    @Test
    void listenersGetTheCurrentResultAtOnceAndOnChange() throws Exception {
        List<String> blockers = new ArrayList<>();

        edt(() -> controller.addListener(r -> blockers.add(String.valueOf(r.blocker()))));
        assertEquals(List.of("No problem selected."), blockers);

        select();

        assertEquals("null", blockers.get(blockers.size() - 1), "last result: no blocker");
    }
}
