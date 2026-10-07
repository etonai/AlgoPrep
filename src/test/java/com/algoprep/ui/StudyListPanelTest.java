package com.algoprep.ui;

import com.algoprep.AppFrame;
import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.studied.StudiedStore;
import com.algoprep.studylist.StudyListModel;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The study list tab, built without a window, and the way it is added to and removed from the right-hand tabs. */
class StudyListPanelTest {

    private static final int RIGHT_PANE_WIDTH = 350;

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private SettingsStore settings;
    private ProblemWorkspace workspace;
    private StudiedStore studied;
    private StudyListModel model;
    private StudyListPanel panel;
    private JTabbedPane tabs;
    private JButton studiedButton;
    private JButton clearButton;
    private JButton uploadButton;
    private Path home;
    private Path listFile;

    private final ChatBridge bridge = new ChatBridge() {
        @Override public void sendPrompt(String prompt, ResponseListener l) { }
        @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
        @Override public void reset() { }
        @Override public void clickUploadFile() { }
    };

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

    private static void layoutAll(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container inner) {
                layoutAll(inner);
            }
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        Path problems = Files.createDirectories(tmp.resolve("problems"));
        for (String key : List.of("0001_two-sum", "0099_no-difficulty", "0105_no-time")) {
            Files.writeString(problems.resolve(key + "_problem.md"), "# " + key);
        }
        home = Files.createDirectories(tmp.resolve("home"));
        listFile = Files.writeString(tmp.resolve("Grind75.csv"),
                "0001_two-sum, Easy, 20 minutes\n"
                + "15_not-found, Tough, 15 minutes\n"
                + "99_no-difficulty, , 2 minutes\n"
                + "105_no-time, Medium, \n");

        settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setProblemsDir(problems.toString());
        settings.setHomeDir(home.toString());
        settings.setStudyListFile(listFile.toString());
        workspace = new ProblemWorkspace(settings, status::add);
        studied = new StudiedStore(Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC), m -> { });
        studied.setFile(StudiedStore.fileFor(home.toString()).orElseThrow());

        StatusReporter reporter = new StatusReporter();
        reporter.attach(status::add);
        AppState appState = new AppState();

        edt(() -> {
            model = new StudyListModel(settings, workspace.catalog(), status::add);
            UploadController uploads = new UploadController(appState, settings, bridge, reporter,
                    workspace.selection(), tmp.resolve("stage").toString());
            StudiedController controls = new StudiedController(settings, workspace.selection(), studied, reporter);
            panel = new StudyListPanel(model, workspace, uploads, controls, studied);
            studiedButton = find(panel, "Studied");
            clearButton = find(panel, "Clear Studied Tag");
            uploadButton = find(panel, "Upload");

            tabs = new JTabbedPane();
            tabs.addTab("MAIN", new JPanel());
            tabs.addTab("Problems", new JPanel());
            tabs.addTab("Settings", new JPanel());
        });
    }

    private JList<?> list() {
        return findList(panel);
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

    private void doubleClickRow(int index) throws Exception {
        // Enter on the highlighted row is the same activation as a double-click
        edt(() -> {
            list().setSelectedIndex(index);
            list().getActionMap().get("activate").actionPerformed(
                    new java.awt.event.ActionEvent(list(), java.awt.event.ActionEvent.ACTION_PERFORMED, "activate"));
        });
        settle();
    }

    private JLabel messageLabel() {
        JLabel[] found = new JLabel[1];
        collectLabels(panel, found);
        return found[0];
    }

    private static void collectLabels(Container root, JLabel[] out) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && l.getText() != null
                    && (l.getText().contains("problem") || l.getText().contains("no files")
                        || l.getText().contains("empty") || l.getText().contains("not found"))
                    && out[0] == null) {
                out[0] = l;
            }
            if (c instanceof Container inner) {
                collectLabels(inner, out);
            }
        }
    }

    // ---- rows ----

    @Test
    void showsTheIdeasFourRowsInOrder() {
        assertEquals(List.of(
                "1 - Two Sum, Easy, 20 minutes",
                "15_not-found, Tough, 15 minutes - NOT FOUND",
                "99 - No Difficulty, 2 minutes",
                "105 - No Time, Medium"), panel.rowTexts());
    }

    @Test
    void selectingAFoundRowSelectsTheProblemEverywhereAndTagsTheRow() throws Exception {
        doubleClickRow(0);

        assertEquals("0001_two-sum", workspace.selection().selectedKey().orElseThrow());
        assertEquals("1 - Two Sum, Easy, 20 minutes   (selected)", panel.rowTexts().get(0));
    }

    @Test
    void aNotFoundRowCannotBeSelectedAndTheMessageSaysWhy() throws Exception {
        doubleClickRow(1);

        assertTrue(workspace.selection().selectedKey().isEmpty());
        assertTrue(messageLabel().getText().contains("15_not-found has no files in the PROBLEMS directory"),
                messageLabel().getText());
    }

    @Test
    void aSingleHighlightDoesNotSelect() throws Exception {
        edt(() -> list().setSelectedIndex(0));
        settle();
        assertTrue(workspace.selection().selectedKey().isEmpty());
    }

    @Test
    void studiedButtonUpdatesTheRowAndTheSummary() throws Exception {
        doubleClickRow(0);
        edt(studiedButton::doClick);
        settle();

        assertEquals("1 - Two Sum, Easy, 20 minutes   (selected)   (STUDIED 2026-10-07)", panel.rowTexts().get(0));
        assertEquals("4 problems, 1 studied, 1 not found", messageLabel().getText());

        edt(clearButton::doClick);
        settle();
        assertEquals("1 - Two Sum, Easy, 20 minutes   (selected)", panel.rowTexts().get(0));
        assertEquals("4 problems, 0 studied, 1 not found", messageLabel().getText());
    }

    @Test
    void theSameProblemSelectedFromAnotherTabIsTheSameSelection() throws Exception {
        edt(() -> workspace.selection().select(workspace.catalog().problems().get(0)));
        settle();

        assertTrue(panel.rowTexts().get(0).contains("(selected)"));
    }

    @Test
    void theButtonsAreTheSharedOnesWithTheSameStateAsOnOtherTabs() throws Exception {
        assertNotNull(uploadButton);
        assertFalse(studiedButton.isEnabled(), "nothing selected");
        assertEquals("No problem selected.", studiedButton.getToolTipText());

        doubleClickRow(2);
        assertTrue(studiedButton.isEnabled());
        assertFalse(clearButton.isEnabled(), "nothing to clear yet");
    }

    @Test
    void theFilterMatchesDifficultyAndShowsHowManyAreShown() throws Exception {
        JTextField filter = findFilter(panel);
        edt(() -> filter.setText("easy"));
        settle();

        assertEquals(List.of("1 - Two Sum, Easy, 20 minutes"), panel.rowTexts());
        assertTrue(messageLabel().getText().startsWith("Showing 1 of 4 problems"), messageLabel().getText());

        edt(() -> filter.setText("zzz"));
        settle();
        assertTrue(panel.rowTexts().isEmpty());
        assertEquals("No problems match the filter.", messageLabel().getText());
    }

    private static JTextField findFilter(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField f) {
                return f;
            }
            if (c instanceof Container inner) {
                JTextField found = findFilter(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void refreshPicksUpAnEditedListAndAProblemThatNowHasFiles() throws Exception {
        Files.writeString(listFile, "15_not-found\n0001_two-sum, Hard\n");
        Files.writeString(tmp.resolve("problems").resolve("0015_not-found_problem.md"), "x");

        edt(() -> find(panel, "Refresh").doClick());
        settle();

        assertEquals(List.of("15 - Not Found", "1 - Two Sum, Hard"), panel.rowTexts());
    }

    @Test
    void anUnreadableFileShowsTheErrorAndAnEmptyList() throws Exception {
        edt(() -> settings.setStudyListFile(tmp.resolve("gone.csv").toString()));
        settle();

        assertTrue(panel.rowTexts().isEmpty());
        assertTrue(messageLabel().getText().contains("not found"), messageLabel().getText());
    }

    @Test
    void longRowsAndTheButtonsFitOrScrollAtTheNormalWidth() throws Exception {
        edt(() -> {
            panel.setSize(RIGHT_PANE_WIDTH, 700);
            layoutAll(panel);
        });
        for (JButton b : List.of(uploadButton, studiedButton, clearButton)) {
            Rectangle r = SwingUtilities.convertRectangle(b.getParent(), b.getBounds(), panel);
            assertTrue(r.x >= 0 && r.x + r.width <= RIGHT_PANE_WIDTH, b.getText() + " is clipped");
        }
    }

    // ---- the tab ----

    private void sync() throws Exception {
        edt(() -> AppFrame.syncStudyListTab(tabs, model, panel, () -> { }));
    }

    @Test
    void theTabAppearsAfterProblemsTitledWithTheFileName() throws Exception {
        sync();

        assertEquals(4, tabs.getTabCount());
        assertEquals("Grind75", tabs.getTitleAt(2));
        assertEquals("Settings", tabs.getTitleAt(3));
        assertSame(panel, tabs.getComponentAt(2));
    }

    @Test
    void noSettingMeansNoTab() throws Exception {
        edt(() -> settings.setStudyListFile(null));
        settle();
        sync();

        assertEquals(3, tabs.getTabCount());
        assertEquals(-1, tabs.indexOfComponent(panel));
    }

    @Test
    void theTabFollowsTheSettingWithoutARestartAndKeepsTheUsersPlace() throws Exception {
        edt(() -> {
            model.addListener(() -> AppFrame.syncStudyListTab(tabs, model, panel, () -> { }));
            tabs.setSelectedIndex(2); // Settings
        });
        edt(() -> settings.setStudyListFile(null));
        settle();
        assertEquals(3, tabs.getTabCount());
        edt(() -> tabs.setSelectedIndex(2)); // Settings again

        edt(() -> settings.setStudyListFile(listFile.toString()));
        settle();

        assertEquals(4, tabs.getTabCount());
        assertEquals("Settings", tabs.getTitleAt(tabs.getSelectedIndex()), "the user stays on Settings");

        edt(() -> settings.setStudyListFile(null));
        settle();
        assertEquals(3, tabs.getTabCount());
        assertEquals("Settings", tabs.getTitleAt(tabs.getSelectedIndex()));
    }

    @Test
    void removingTheTabWhileItIsSelectedGoesToProblems() throws Exception {
        sync();
        edt(() -> tabs.setSelectedIndex(2));

        edt(() -> settings.setStudyListFile(null));
        settle();
        sync();

        assertEquals(3, tabs.getTabCount());
        assertEquals("Problems", tabs.getTitleAt(tabs.getSelectedIndex()));
    }

    @Test
    void aDifferentFileRetitlesTheExistingTab() throws Exception {
        sync();
        Path other = Files.writeString(tmp.resolve("Top20.csv"), "0001_two-sum\n");

        edt(() -> settings.setStudyListFile(other.toString()));
        settle();
        sync();

        assertEquals(4, tabs.getTabCount());
        assertEquals("Top20", tabs.getTitleAt(2));
    }
}
