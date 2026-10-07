package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.DocumentLoader;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import java.util.Objects;

/**
 * Left pane (Plan section 7): three fixed, read-only tabs, Problem, Notes and My Notes, each
 * showing a file as rendered Markdown or a short message.
 *
 * <p>Files are re-read when the selection changes and whenever a tab is selected, so edits made in
 * another editor appear without a file watcher. Reading happens on the Swing thread: the files
 * are small local files with a size cap, and a watcher or background loading is not worth it for
 * a personal tool. Nothing here depends on the browser.
 */
public class DisplayPanel extends JTabbedPane {

    private static final int PROBLEM = 0;
    private static final int NOTES = 1;
    private static final int MY_NOTES = 2;

    private final SelectedProblemModel selection;
    private final SettingsStore settings;
    private final MarkdownView[] views;
    private String lastHome;

    public DisplayPanel(SelectedProblemModel selection, SettingsStore settings,
                        NativeThemeModel themeModel) {
        this.selection = selection;
        this.settings = settings;
        this.views = new MarkdownView[] {
                new MarkdownView(themeModel), new MarkdownView(themeModel), new MarkdownView(themeModel)};
        addTab("Problem", views[PROBLEM]);
        addTab("Notes", views[NOTES]);
        addTab("My Notes", views[MY_NOTES]);

        selection.addListener(() -> UiThread.run(this::reloadAll));
        settings.addListener(() -> UiThread.run(this::onSettingsChanged));
        // Re-read the file for whichever tab the user switches to
        addChangeListener(e -> reload(getSelectedIndex()));

        lastHome = settings.getHomeDir();
        reloadAll();
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
