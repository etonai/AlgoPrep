package com.algoprep.ui;

import com.algoprep.AppFrame;
import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.studied.StudiedStore;
import com.algoprep.studylist.StudyListModel;
import com.algoprep.studylist.StudyListRow;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeApplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The study list tab must follow the dark/light setting like the other tabs. The theme applier
 * recolors only the components that are in the window when it runs, and the tab is added later,
 * when a Study List File is set, so adding it has to apply the current theme again.
 */
class StudyListThemeTest {

    @TempDir
    Path tmp;

    private final NativeThemeApplier applier = new NativeThemeApplier();
    private JFrame frame;
    private JTabbedPane tabs;
    private ProblemsPanel problemsPanel;
    private StudyListPanel studyPanel;
    private StudyListModel model;
    private NativeTheme current = NativeTheme.DARK;

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        Files.writeString(problems.resolve("0001_two-sum_problem.md"), "x");
        Path list = Files.writeString(tmp.resolve("Grind75.csv"), "0001_two-sum, Easy\n15_not-found, Tough\n");

        SettingsStore settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setProblemsDir(problems.toString());
        settings.setStudyListFile(list.toString());
        ProblemWorkspace workspace = new ProblemWorkspace(settings, m -> { });
        StudiedStore studied = new StudiedStore(Clock.systemDefaultZone(), m -> { });
        StatusReporter reporter = new StatusReporter();
        reporter.attach(m -> { });
        ChatBridge bridge = new ChatBridge() {
            @Override public void sendPrompt(String prompt, ResponseListener l) { }
            @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
            @Override public void reset() { }
            @Override public void clickUploadFile() { }
        };

        edt(() -> {
            AppState appState = new AppState();
            UploadController uploads = new UploadController(appState, settings, bridge, reporter,
                    workspace.selection(), tmp.resolve("stage").toString());
            StudiedController controls = new StudiedController(settings, workspace.selection(), studied, reporter);
            model = new StudyListModel(settings, workspace.catalog(), m -> { });
            problemsPanel = new ProblemsPanel(workspace, uploads, controls, studied);
            studyPanel = new StudyListPanel(model, workspace, uploads, controls, studied);

            // The same order as the real window: MAIN, Problems, (study list), Settings
            tabs = new JTabbedPane();
            tabs.addTab("MAIN", new JPanel());
            tabs.addTab("Problems", problemsPanel);
            tabs.addTab("Settings", new JPanel());
            frame = new JFrame();
            frame.add(tabs);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        edt(() -> frame.dispose());
    }

    private void applyTheme(NativeTheme theme) throws Exception {
        current = theme;
        edt(() -> applier.apply(frame, theme));
    }

    /** Adds the tab the way AppFrame does: inserting it applies the current theme again. */
    private void addStudyListTab() throws Exception {
        edt(() -> AppFrame.syncStudyListTab(tabs, model, studyPanel, () -> applier.apply(frame, current)));
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                return type.cast(c);
            }
            if (c instanceof Container inner) {
                T found = first(inner, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton button(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container inner) {
                JButton found = button(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void assertSameColors(Function<Container, Color> pick, String what, Container a, Container b) {
        assertEquals(pick.apply(a), pick.apply(b), what);
    }

    /** The study list tab looks like the Problems tab: panel, filter box, buttons and list. */
    private void assertLooksLikeTheProblemsTab(NativeTheme theme) {
        String mode = " (" + theme + ")";
        assertSameColors(c -> c.getBackground(), "panel background" + mode, studyPanel, problemsPanel);
        assertSameColors(c -> first(c, JTextField.class).getBackground(), "filter background" + mode,
                studyPanel, problemsPanel);
        assertSameColors(c -> first(c, JTextField.class).getForeground(), "filter text" + mode,
                studyPanel, problemsPanel);
        assertSameColors(c -> button(c, "Refresh").getBackground(), "button background" + mode,
                studyPanel, problemsPanel);
        assertSameColors(c -> button(c, "Refresh").getForeground(), "button text" + mode,
                studyPanel, problemsPanel);
        assertSameColors(c -> first(c, JViewport.class).getBackground(), "list viewport" + mode,
                studyPanel, problemsPanel);
        assertSameColors(c -> first(c, JLabel.class).getForeground(), "label text" + mode,
                studyPanel, problemsPanel);
    }

    @Test
    void aTabAddedAfterTheThemeWasAppliedLooksLikeTheOthersInBothModes() throws Exception {
        for (NativeTheme theme : NativeTheme.values()) {
            applyTheme(theme);
            addStudyListTab();

            assertLooksLikeTheProblemsTab(theme);

            edt(() -> tabs.remove(studyPanel)); // back to "no list set" for the next mode
        }
    }

    @Test
    void aThemeSwitchWhileTheTabIsAbsentIsNotMissedWhenItIsAddedLater() throws Exception {
        applyTheme(NativeTheme.DARK);
        applyTheme(NativeTheme.LIGHT); // the tab does not exist yet
        addStudyListTab();

        assertLooksLikeTheProblemsTab(NativeTheme.LIGHT);
        assertEquals(problemsPanel.getBackground(), studyPanel.getBackground());
    }

    @Test
    void aThemeSwitchWhileTheTabIsPresentUpdatesIt() throws Exception {
        applyTheme(NativeTheme.DARK);
        addStudyListTab();
        Color dark = studyPanel.getBackground();

        applyTheme(NativeTheme.LIGHT);

        assertNotEquals(dark, studyPanel.getBackground(), "the colors changed with the theme");
        assertLooksLikeTheProblemsTab(NativeTheme.LIGHT);
    }

    @Test
    void notFoundRowsStayReadableAgainstTheListInBothModes() throws Exception {
        for (NativeTheme theme : NativeTheme.values()) {
            applyTheme(theme);
            addStudyListTab();

            @SuppressWarnings("unchecked")
            JList<StudyListRow> list = first(studyPanel, JList.class);
            StudyListRow notFound = model.rows().stream().filter(r -> !r.isFound()).findFirst().orElseThrow();
            Component cell = list.getCellRenderer().getListCellRendererComponent(list, notFound, 1, false, false);

            double ratio = contrast(cell.getForeground(), list.getBackground());
            assertTrue(ratio >= 3.0, theme + ": NOT FOUND text contrast is only " + ratio);

            edt(() -> tabs.remove(studyPanel));
        }
    }

    // ---- WCAG contrast ratio ----

    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        List<Integer> parts = List.of(c.getRed(), c.getGreen(), c.getBlue());
        double[] v = new double[3];
        for (int i = 0; i < 3; i++) {
            double s = parts.get(i) / 255.0;
            v[i] = s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }
}
