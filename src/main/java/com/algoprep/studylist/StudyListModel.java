package com.algoprep.studylist;

import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemCatalog;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The loaded study list, resolved against the PROBLEMS catalog. No Swing.
 *
 * <p>It follows the Study List File setting (set, change, clear), re-resolves when the catalog is
 * rescanned, and re-reads the file on {@link #reload}. A file that cannot be read leaves an empty
 * list and an error message, so the tab can show why. Listeners are told only about real changes.
 * AlgoPrep only reads the list file, it never writes it.
 */
public final class StudyListModel {

    private record State(String title, List<StudyListEntry> entries, int skipped, String error,
                         List<StudyListRow> rows) { }

    private static final State INACTIVE = new State(null, List.of(), 0, null, List.of());

    private final SettingsStore settings;
    private final ProblemCatalog catalog;
    private final Consumer<String> status;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private String settingText;
    private State state = INACTIVE;

    /** @param status receives read errors and skipped-line counts; may be called from this constructor */
    public StudyListModel(SettingsStore settings, ProblemCatalog catalog, Consumer<String> status) {
        this.settings = settings;
        this.catalog = catalog;
        this.status = status != null ? status : s -> { };
        this.settingText = settings.getStudyListFile();
        this.state = load(true);

        settings.addListener(this::onSettingsChanged);
        catalog.addListener(this::onCatalogChanged);
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    /** Whether a study list file is set. The tab exists only while this is true. */
    public synchronized boolean isActive() { return state.title() != null; }

    /** The tab title (the file name without its extension), or empty if no list is set. */
    public synchronized Optional<String> title() { return Optional.ofNullable(state.title()); }

    /** The rows in the file's order. Empty if the file could not be read. */
    public synchronized List<StudyListRow> rows() { return state.rows(); }

    /** Why the file could not be read, or null. */
    public synchronized String error() { return state.error(); }

    /** How many lines of the file could not be used. */
    public synchronized int skipped() { return state.skipped(); }

    /** The saved setting as text, or null. Kept even when the path is bad. */
    public synchronized String settingText() { return settingText; }

    /** Re-reads the file and reports problems, for the Refresh button. */
    public void reload() {
        reload(true);
    }

    /** Re-reads the file without repeating messages, for when the tab is shown. */
    public void reloadQuietly() {
        reload(false);
    }

    // ---- internals ----

    private void reload(boolean report) {
        State next = load(report);
        replace(next);
    }

    private void onSettingsChanged() {
        String now = settings.getStudyListFile();
        synchronized (this) {
            if (Objects.equals(now, settingText)) {
                return;
            }
            settingText = now;
        }
        reload(true);
    }

    private void onCatalogChanged() {
        State current;
        synchronized (this) {
            current = state;
        }
        List<StudyListRow> rows = StudyListResolver.resolve(current.entries(), catalog.problems());
        replace(new State(current.title(), current.entries(), current.skipped(), current.error(), rows));
    }

    private void replace(State next) {
        synchronized (this) {
            if (next.equals(state)) {
                return;
            }
            state = next;
        }
        listeners.forEach(Runnable::run);
    }

    private State load(boolean report) {
        String text;
        synchronized (this) {
            text = settingText;
        }
        if (text == null || text.isBlank()) {
            return INACTIVE;
        }
        Path file;
        try {
            file = Path.of(text);
        } catch (InvalidPathException e) {
            String message = "The study list setting is not a valid path: " + text;
            if (report) {
                status.accept(message);
            }
            return new State(text, List.of(), 0, message, List.of());
        }

        String title = StudyListTitle.of(file);
        StudyListFile.Result result = StudyListFile.load(file);
        if (report) {
            if (!result.ok()) {
                status.accept(result.error());
            } else if (result.skipped() > 0) {
                status.accept("Skipped " + result.skipped() + " unreadable line(s) in the study list "
                        + file.getFileName() + ".");
            }
        }
        List<StudyListRow> rows = StudyListResolver.resolve(result.entries(), catalog.problems());
        return new State(title, result.entries(), result.skipped(), result.error(), rows);
    }
}
