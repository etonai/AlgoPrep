package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.DocumentLoader;
import com.algoprep.display.FontScaleModel;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Left pane (Plan section 7): three fixed, read-only tabs, Problem, Notes and My Notes, each
 * showing a file as rendered Markdown or a short message. A small bar above the tabs has + and -
 * buttons that change the text size of all three tabs together.
 *
 * <p>Files are re-read when the selection changes and whenever a tab is selected, so edits made in
 * another editor appear without a file watcher. Reading happens on the Swing thread: the files
 * are small local files with a size cap, and a watcher or background loading is not worth it for
 * a personal tool. Nothing here depends on the browser.
 */
public class DisplayPanel extends JPanel {

    private static final int PROBLEM = 0;
    private static final int NOTES = 1;
    private static final int MY_NOTES = 2;

    private final SelectedProblemModel selection;
    private final SettingsStore settings;
    private final FontScaleModel fontScale;
    private final JTabbedPane tabs = new JTabbedPane();
    private final MarkdownView[] views;
    private final JButton smaller = new JButton("-");
    private final JButton larger = new JButton("+");
    private String lastHome;

    public DisplayPanel(SelectedProblemModel selection, SettingsStore settings,
                        NativeThemeModel themeModel, FontScaleModel fontScale) {
        super(new BorderLayout());
        this.selection = selection;
        this.settings = settings;
        this.fontScale = fontScale;
        // One scale model for all three views, so the tabs always share a size
        this.views = new MarkdownView[] {
                new MarkdownView(themeModel, fontScale),
                new MarkdownView(themeModel, fontScale),
                new MarkdownView(themeModel, fontScale)};
        tabs.addTab("Problem", views[PROBLEM]);
        tabs.addTab("Notes", views[NOTES]);
        tabs.addTab("My Notes", views[MY_NOTES]);

        add(sizeBar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        selection.addListener(() -> UiThread.run(this::reloadAll));
        settings.addListener(() -> UiThread.run(this::onSettingsChanged));
        // Re-read the file for whichever tab the user switches to
        tabs.addChangeListener(e -> reload(tabs.getSelectedIndex()));

        lastHome = settings.getHomeDir();
        reloadAll();
    }

    /** The + and - buttons, right-aligned above the tabs so they show whichever tab is open. */
    private JPanel sizeBar() {
        for (JButton b : new JButton[] {smaller, larger}) {
            b.setMargin(new Insets(0, 10, 0, 10));
            b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
            b.setFocusable(false); // keeps focus where it was, as in the browser pane
        }
        smaller.addActionListener(e -> fontScale.decrease());
        larger.addActionListener(e -> fontScale.increase());
        fontScale.addListener(() -> UiThread.run(this::updateSizeButtons));
        updateSizeButtons();

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        bar.add(smaller);
        bar.add(larger);
        return bar;
    }

    /** Disables a button at its limit, and shows the current size in the tooltips. */
    private void updateSizeButtons() {
        String size = " (now " + fontScale.percent() + "%)";
        smaller.setEnabled(fontScale.canDecrease());
        larger.setEnabled(fontScale.canIncrease());
        smaller.setToolTipText(fontScale.canDecrease() ? "Smaller text" + size : "Smallest size" + size);
        larger.setToolTipText(fontScale.canIncrease() ? "Larger text" + size : "Largest size" + size);
    }

    private void onSettingsChanged() {
        String home = settings.getHomeDir();
        if (!Objects.equals(home, lastHome)) {
            lastHome = home;
            reload(MY_NOTES);
        }
    }

    private void reloadAll() {
        for (int i = 0; i < views.length; i++) {
            reload(i);
        }
    }

    private void reload(int tab) {
        if (tab < 0 || tab >= views.length) {
            return;
        }
        DocumentLoader.Doc doc = switch (tab) {
            case PROBLEM -> DocumentLoader.problem(selection.current(), selection.selectedKey());
            case NOTES -> DocumentLoader.notes(selection.current(), selection.selectedKey());
            default -> DocumentLoader.myNotes(selection.selectedKey(), settings.getHomeDir());
        };
        if (doc.isText()) {
            views[tab].showMarkdown(doc.markdown());
        } else {
            views[tab].showMessage(doc.message());
        }
    }
}
