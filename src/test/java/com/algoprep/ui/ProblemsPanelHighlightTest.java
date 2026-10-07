package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.studied.StudiedStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Opening the Problems tab highlights the selected problem, as if the user had clicked it. */
class ProblemsPanelHighlightTest {

    @TempDir
    Path tmp;

    private ProblemWorkspace workspace;
    private ProblemsPanel panel;

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        for (String key : List.of("0001_two-sum", "0002_add-two", "0003_longest")) {
            Files.writeString(problems.resolve(key + "_problem.md"), "# " + key);
        }
        Path home = Files.createDirectories(tmp.resolve("home"));
        SettingsStore settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setProblemsDir(problems.toString());
        settings.setHomeDir(home.toString());
        workspace = new ProblemWorkspace(settings, m -> { });
        StudiedStore studied = new StudiedStore(Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC), m -> { });
        studied.setFile(StudiedStore.fileFor(home.toString()).orElseThrow());
        StatusReporter reporter = new StatusReporter();
        reporter.attach(m -> { });
        ChatBridge bridge = new ChatBridge() {
            @Override public void sendPrompt(String prompt, ResponseListener l) { }
            @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
            @Override public void reset() { }
            @Override public void clickUploadFile() { }
        };
        edt(() -> {
            UploadController uploads = new UploadController(new AppState(), settings, bridge, reporter,
                    workspace.selection(), tmp.resolve("stage").toString());
            StudiedController controls = new StudiedController(settings, workspace.selection(), studied,
                    reporter, (p, m) -> true);
            panel = new ProblemsPanel(workspace, uploads, controls, studied);
        });
    }

    private static JList<?> findList(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JList<?> l) {
                return l;
            }
            if (c instanceof Container inner) {
                JList<?> found = findList(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private int highlightedAfterOpeningTheTab() throws Exception {
        JFrame[] frame = new JFrame[1];
        try {
            edt(() -> {
                JTabbedPane host = new JTabbedPane();
                host.addTab("MAIN", new JPanel());
                host.addTab("Problems", panel);
                frame[0] = new JFrame();
                frame[0].add(host);
                frame[0].setSize(400, 500);
                frame[0].setVisible(true);
                host.setSelectedIndex(1);
            });
            edt(() -> { });
            edt(() -> { });
            int[] index = new int[1];
            edt(() -> index[0] = findList(panel).getSelectedIndex());
            return index[0];
        } finally {
            edt(() -> frame[0].dispose());
        }
    }

    @Test
    void openingTheTabHighlightsTheSelectedProblem() throws Exception {
        edt(() -> workspace.selection().select(workspace.catalog().problems().get(2)));
        edt(() -> { });

        assertEquals(2, highlightedAfterOpeningTheTab());
        assertEquals("0003_longest", workspace.selection().selectedKey().orElseThrow());
    }

    @Test
    void openingTheTabWithNothingSelectedHighlightsNothing() throws Exception {
        assertEquals(-1, highlightedAfterOpeningTheTab());
        assertTrue(workspace.selection().selectedKey().isEmpty());
    }
}
