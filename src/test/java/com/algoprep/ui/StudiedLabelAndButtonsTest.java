package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.FontScaleModel;
import com.algoprep.problem.Problem;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedLabel;
import com.algoprep.studied.StudiedStore;
import com.algoprep.theme.NativeThemeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The STUDIED label in the display window, and the renamed Clear Studied Tag button on both tabs. */
class StudiedLabelAndButtonsTest {

    /** About the width of the right-hand pane at the normal window size (1400 - 420 - 630). */
    private static final int RIGHT_PANE_WIDTH = 350;

    @TempDir
    Path tmp;

    private SettingsStore settings;
    private SelectedProblemModel selection;
    private StudiedStore store;
    private Problem problem;
    private StatusReporter reporter;
    private StudiedController controller;

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static void settle() throws Exception {
        edt(() -> { });
    }

    private static JButton find(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container inner) {
                JButton found = find(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JLabel findLabelStartingWith(Container root, String prefix) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && l.isVisible() && l.getText() != null && l.getText().startsWith(prefix)) {
                return l;
            }
            if (c instanceof Container inner) {
                JLabel found = findLabelStartingWith(inner, prefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void layoutAll(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container inner) {
                layoutAll(inner);
            }
        }
    }

    private static void assertFitsWidth(JComponent root, JButton button, int width) {
        Rectangle r = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), root);
        assertTrue(r.x >= 0 && r.x + r.width <= width,
                "'" + button.getText() + "' spans " + r.x + ".." + (r.x + r.width) + " in a " + width + " px pane");
    }

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        Path statement = Files.writeString(problems.resolve("0001_two-sum_problem.md"), "# Two Sum");
        problem = new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.empty());
        Path home = Files.createDirectories(tmp.resolve("home"));

        settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setHomeDir(home.toString());
        settings.setProblemsDir(problems.toString());
        selection = new SelectedProblemModel();
        store = new StudiedStore(Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC), m -> { });
        store.setFile(StudiedStore.fileFor(home.toString()).orElseThrow());
        reporter = new StatusReporter();
        reporter.attach(m -> { });
        edt(() -> controller = new StudiedController(settings, selection, store, reporter, (p, m) -> true));
    }

    // ---- the label ----

    @Test
    void labelTextIsPureAndOnlyForAStudiedProblem() {
        assertEquals(Optional.of("STUDIED 2026-10-07"), StudiedLabel.text(Optional.of(LocalDate.of(2026, 10, 7))));
        assertEquals(Optional.empty(), StudiedLabel.text(Optional.empty()));
    }

    private DisplayPanel displayPanel() throws Exception {
        DisplayPanel[] holder = new DisplayPanel[1];
        edt(() -> holder[0] = new DisplayPanel(selection, settings, new NativeThemeModel(),
                new FontScaleModel(), store));
        return holder[0];
    }

    @Test
    void labelAppearsAndDisappearsAsTheStudiedDateChanges() throws Exception {
        DisplayPanel panel = displayPanel();
        assertNull(findLabelStartingWith(panel, "STUDIED"), "nothing selected: no label");

        edt(() -> selection.select(problem));
        settle();
        assertNull(findLabelStartingWith(panel, "STUDIED"), "selected but not studied: no label");

        edt(() -> store.markStudied(problem.key()));
        settle();
        JLabel label = findLabelStartingWith(panel, "STUDIED");
        assertNotNull(label);
        assertEquals("STUDIED 2026-10-07", label.getText());

        // It is inside the Problem tab, not in the bar above the tabs, and it is large and readable
        JTabbedPane tabs = (JTabbedPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertTrue(SwingUtilities.isDescendingFrom(label, tabs.getComponentAt(0)), "in the Problem tab");
        assertFalse(SwingUtilities.isDescendingFrom(label, tabs.getComponentAt(1)));
        assertTrue(label.getFont().getSize() >= 20 && label.getFont().isBold());
        assertNotEquals(label.getForeground(), label.getBackground());
        assertTrue(label.getPreferredSize().width <= 400,
                "fits the left pane (420 px): " + label.getPreferredSize().width);

        edt(() -> store.clear(problem.key()));
        settle();
        assertNull(findLabelStartingWith(panel, "STUDIED"), "cleared: label gone");
    }

    @Test
    void bannerKeepsItsColorsWhenTheThemeIsApplied() throws Exception {
        DisplayPanel panel = displayPanel();
        edt(() -> selection.select(problem));
        edt(() -> store.markStudied(problem.key()));
        settle();
        JLabel label = findLabelStartingWith(panel, "STUDIED");
        Color before = label.getBackground();

        // The theme applier recolors every label, so the banner must ignore it
        edt(() -> {
            label.setBackground(Color.BLACK);
            label.setForeground(Color.BLACK);
        });

        assertEquals(before, label.getBackground());
        assertEquals(new Color(34, 34, 34), label.getForeground());
    }

    @Test
    void labelFollowsTheStudiedButtonsAndTheSelection() throws Exception {
        DisplayPanel panel = displayPanel();
        JButton studied = new JButton("Studied");
        JButton clear = new JButton("Clear Studied Tag");
        edt(() -> {
            controller.bindStudied(studied);
            controller.bindClear(clear);
            selection.select(problem);
        });
        settle();

        edt(studied::doClick);
        settle();
        assertNotNull(findLabelStartingWith(panel, "STUDIED 2026-10-07"));

        edt(selection::clear);
        settle();
        assertNull(findLabelStartingWith(panel, "STUDIED"), "no problem selected: no label");

        edt(() -> selection.select(problem));
        settle();
        edt(clear::doClick);
        settle();
        assertNull(findLabelStartingWith(panel, "STUDIED"));
    }

    @Test
    void labelShowsWhenTheStudiedFileIsEditedAndReloaded() throws Exception {
        DisplayPanel panel = displayPanel();
        edt(() -> selection.select(problem));
        settle();

        Files.writeString(store.file().orElseThrow(), "key,date\n0001_two-sum,2026-03-04\n");
        edt(store::reload);
        settle();

        assertNotNull(findLabelStartingWith(panel, "STUDIED 2026-03-04"));
    }

    @Test
    void labelShowsEvenWhenTheStatementIsGone() throws Exception {
        DisplayPanel panel = displayPanel();
        edt(() -> selection.select(problem));
        edt(() -> store.markStudied(problem.key()));
        edt(() -> selection.reconcile(java.util.List.of())); // statement gone: unavailable
        settle();

        assertTrue(selection.isUnavailable());
        assertNotNull(findLabelStartingWith(panel, "STUDIED 2026-10-07"));
    }

    // ---- the renamed button ----

    private MainPanel mainPanel() throws Exception {
        AppState appState = new AppState();
        ChatBridge bridge = new ChatBridge() {
            @Override public void sendPrompt(String prompt, ResponseListener l) { }
            @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
            @Override public void reset() { }
            @Override public void clickUploadFile() { }
        };
        MainPanel[] holder = new MainPanel[1];
        edt(() -> {
            UploadController uploads = new UploadController(appState, settings, bridge, reporter,
                    selection, tmp.resolve("stage").toString());
            holder[0] = new MainPanel(appState, settings, bridge, reporter, selection, uploads, controller);
        });
        return holder[0];
    }

    @Test
    void mainTabHasClearStudiedTagAndItFits() throws Exception {
        MainPanel panel = mainPanel();

        assertNull(find(panel, "Clear"), "the old name is gone");
        JButton clear = find(panel, "Clear Studied Tag");
        JButton studied = find(panel, "Studied");
        JButton upload = find(panel, "Upload");
        assertNotNull(clear);
        assertNotNull(studied);
        assertNotNull(upload);

        edt(() -> {
            panel.setSize(RIGHT_PANE_WIDTH, 700);
            layoutAll(panel);
        });
        assertFitsWidth(panel, upload, RIGHT_PANE_WIDTH);
        assertFitsWidth(panel, studied, RIGHT_PANE_WIDTH);
        assertFitsWidth(panel, clear, RIGHT_PANE_WIDTH);
    }

    @Test
    void problemsTabHasClearStudiedTagAndItFits() throws Exception {
        ProblemWorkspace workspace = new ProblemWorkspace(settings, m -> { });
        AppState appState = new AppState();
        ChatBridge bridge = new ChatBridge() {
            @Override public void sendPrompt(String prompt, ResponseListener l) { }
            @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
            @Override public void reset() { }
            @Override public void clickUploadFile() { }
        };
        ProblemsPanel[] holder = new ProblemsPanel[1];
        edt(() -> {
            UploadController uploads = new UploadController(appState, settings, bridge, reporter,
                    workspace.selection(), tmp.resolve("stage").toString());
            StudiedController studiedControls = new StudiedController(settings, workspace.selection(),
                    store, reporter);
            holder[0] = new ProblemsPanel(workspace, uploads, studiedControls, store);
        });
        ProblemsPanel panel = holder[0];

        assertNull(find(panel, "Clear"), "the old name is gone");
        JButton clear = find(panel, "Clear Studied Tag");
        assertNotNull(clear);
        assertNotNull(find(panel, "Studied"));

        edt(() -> {
            panel.setSize(RIGHT_PANE_WIDTH, 700);
            layoutAll(panel);
        });
        assertFitsWidth(panel, find(panel, "Upload"), RIGHT_PANE_WIDTH);
        assertFitsWidth(panel, find(panel, "Studied"), RIGHT_PANE_WIDTH);
        assertFitsWidth(panel, clear, RIGHT_PANE_WIDTH);
    }

    @Test
    void clearStudiedTagTooltipsAndMessageUseTheNewWording() throws Exception {
        JButton clear = new JButton("Clear Studied Tag");
        java.util.List<String> messages = new java.util.ArrayList<>();
        StatusReporter local = new StatusReporter();
        local.attach(messages::add);
        edt(() -> {
            StudiedController c = new StudiedController(settings, selection, store, local, (p, m) -> true);
            c.bindClear(clear);
            selection.select(problem);
        });
        edt(() -> store.markStudied(problem.key()));
        settle();

        assertTrue(clear.getToolTipText().contains("studied tag"), clear.getToolTipText());
        edt(clear::doClick);
        assertEquals(java.util.List.of("Cleared the studied tag for 1 - Two Sum."), messages);
    }
}
