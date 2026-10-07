package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemNames;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedAvailability;
import com.algoprep.studied.StudiedPrompt;
import com.algoprep.studied.StudiedStore;

import javax.swing.*;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The one Studied implementation, shared by every Studied and Clear button (the MAIN tab and the
 * Problems tab), in the style of {@link UploadController}. It owns whether each button is allowed
 * and why not, and the calls to {@link StudiedStore}. Both act on the <em>selected</em> problem.
 * Must be used on the Swing thread.
 */
public class StudiedController {

    private final SettingsStore settings;
    private final SelectedProblemModel selection;
    private final StudiedStore store;
    private final StatusReporter status;

    private final List<JButton> studiedButtons = new ArrayList<>();
    private final List<JButton> clearButtons = new ArrayList<>();

    private StudiedAvailability.Result latest;

    /** Asks the user a yes/no question. The parent is the window of the button that was pressed. */
    @FunctionalInterface
    public interface Confirmer {
        boolean confirm(Component parent, String message);

        /** Same, saying which answer Enter gives. Stubs may ignore it. */
        default boolean confirm(Component parent, String message, boolean defaultYes) {
            return confirm(parent, message);
        }
    }

    /** The real dialog: Yes and No. Escape or closing the window counts as No. */
    static final class DialogConfirmer implements Confirmer {
        @Override
        public boolean confirm(Component parent, String message) {
            return confirm(parent, message, false);
        }

        @Override
        public boolean confirm(Component parent, String message, boolean defaultYes) {
            Object[] options = {"Yes", "No"};
            int answer = JOptionPane.showOptionDialog(parent, message, "Are you sure?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
                    defaultYes ? options[0] : options[1]);
            return answer == 0;
        }
    }

    private final Confirmer confirmer;
    private final ClipboardWriter clipboard;

    public StudiedController(SettingsStore settings, SelectedProblemModel selection,
                             StudiedStore store, StatusReporter status) {
        this(settings, selection, store, status, new DialogConfirmer(), ClipboardWriter.SYSTEM);
    }

    public StudiedController(SettingsStore settings, SelectedProblemModel selection,
                             StudiedStore store, StatusReporter status, Confirmer confirmer) {
        this(settings, selection, store, status, confirmer, ClipboardWriter.SYSTEM);
    }

    public StudiedController(SettingsStore settings, SelectedProblemModel selection,
                             StudiedStore store, StatusReporter status, Confirmer confirmer,
                             ClipboardWriter clipboard) {
        this.confirmer = confirmer;
        this.clipboard = clipboard;
        this.settings = settings;
        this.selection = selection;
        this.store = store;
        this.status = status;

        settings.addListener(() -> UiThread.run(this::refresh));
        selection.addListener(() -> UiThread.run(this::refresh));
        store.addListener(() -> UiThread.run(this::refresh));

        refresh();
    }

    /** Attaches a Studied button: pressing it marks the selected problem as studied today. */
    public void bindStudied(JButton button) {
        button.addActionListener(e -> markStudied(button));
        studiedButtons.add(button);
        apply();
    }

    /** Attaches a Clear button: pressing it removes the selected problem's studied date. */
    public void bindClear(JButton button) {
        button.addActionListener(e -> clear(button));
        clearButtons.add(button);
        apply();
    }

    /** One trigger for every Studied button, for a future shortcut. */
    public void trigger() {
        if (latest.canMark()) {
            markStudied(studiedButtons.isEmpty() ? null : studiedButtons.get(0));
        }
    }

    // ---- behavior ----

    private void markStudied(JButton source) {
        Optional<String> key = selection.selectedKey();
        if (key.isEmpty() || !latest.canMark()) {
            return;
        }
        // A failed save has already been reported by the store, and the old date stays in place
        if (store.markStudied(key.get())) {
            String name = ProblemNames.displayForKey(key.get());
            status.report("Marked " + name + " as studied on "
                    + store.studiedOn(key.get()).map(Object::toString).orElse("today") + ".");
            offerPrompt(key.get(), source);
        }
    }

    /** Asks whether to copy the Studied prompt, after the date is recorded. Yes is the default. */
    private void offerPrompt(String key, JButton source) {
        Optional<String> prompt = StudiedPrompt.forKey(key);
        if (prompt.isEmpty()) {
            return;
        }
        Component parent = source == null ? null : SwingUtilities.getWindowAncestor(source);
        if (!confirmer.confirm(parent, "Copy Studied prompt to clipboard?", true)) {
            return;
        }
        try {
            clipboard.copy(prompt.get());
            status.report("Copied \"" + prompt.get() + "\" to the clipboard.");
        } catch (Exception | LinkageError e) {
            status.report("Could not copy to the clipboard: " + e.getMessage());
        }
    }

    private void clear(JButton source) {
        Optional<String> key = selection.selectedKey();
        if (key.isEmpty() || !latest.canClear()) {
            return;
        }
        String question = "Clear the studied tag for " + ProblemNames.displayForKey(key.get()) + "?";
        if (!confirmer.confirm(SwingUtilities.getWindowAncestor(source), question)) {
            return;
        }
        if (store.clear(key.get())) {
            status.report("Cleared the studied tag for " + ProblemNames.displayForKey(key.get()) + ".");
        }
    }

    private void refresh() {
        Optional<String> key = selection.selectedKey();
        latest = StudiedAvailability.evaluate(selection.current(), key, settings.getHomeDir(),
                key.flatMap(store::studiedOn));
        apply();
    }

    private void apply() {
        studiedButtons.forEach(b -> {
            b.setEnabled(latest.canMark());
            b.setToolTipText(studiedTooltip());
        });
        clearButtons.forEach(b -> {
            b.setEnabled(latest.canClear());
            b.setToolTipText(latest.canClear()
                    ? "Remove the studied tag (date) for the selected problem" : latest.clearBlocker());
        });
    }

    private String studiedTooltip() {
        if (!latest.canMark()) {
            return latest.markBlocker();
        }
        return latest.date()
                .map(d -> "Studied " + d + ". Press to update to today.")
                .orElse("Mark the selected problem as studied today");
    }
}
