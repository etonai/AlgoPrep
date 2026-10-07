package com.algoprep.problem;

import com.algoprep.config.SettingsStore;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Ties the catalog, the selection and the saved settings together, so the rules live in one
 * testable place (Plan sections 5.3 and 10):
 * <ul>
 *   <li>scan at startup, when the PROBLEMS directory changes, and on refresh;</li>
 *   <li>restore the last selected key at startup only if the scan finds it;</li>
 *   <li>changing the directory clears the selection;</li>
 *   <li>the selected key is saved whenever the selection changes.</li>
 * </ul>
 */
public final class ProblemWorkspace {

    private final SettingsStore settings;
    private final Consumer<String> status;
    private final ProblemCatalog catalog = new ProblemCatalog();
    private final SelectedProblemModel selection = new SelectedProblemModel();

    /** @param status receives scan errors and results; may be called from this constructor */
    public ProblemWorkspace(SettingsStore settings, Consumer<String> status) {
        this.settings = settings;
        this.status = status;

        selection.addListener(() -> settings.setLastSelectedKey(selection.selectedKey().orElse(null)));
        catalog.addListener(() -> selection.reconcile(catalog.problems()));

        String saved = settings.getProblemsDir();
        if (saved != null && !saved.isBlank()) {
            Path dir = toPath(saved);
            if (dir != null) {
                catalog.setDirectory(dir);
                // A stale saved key is left in the settings until the user selects something
                selection.restore(settings.getLastSelectedKey(), catalog.problems());
                if (catalog.lastError() != null) {
                    status.accept(catalog.lastError());
                }
            }
        }
    }

    public ProblemCatalog catalog()          { return catalog; }
    public SelectedProblemModel selection()  { return selection; }

    /** The saved PROBLEMS setting as text, or null if unset. Kept even if the path is bad. */
    public String directoryText()            { return settings.getProblemsDir(); }

    /** Choosing a different directory clears the selection. Choosing the same one only rescans. */
    public void setDirectory(Path newDirectory) {
        String text = newDirectory == null ? null : newDirectory.toString();
        if (Objects.equals(text, settings.getProblemsDir())) {
            refresh();
            return;
        }
        settings.setProblemsDir(text);
        selection.clear();
        catalog.setDirectory(newDirectory);
        reportScan();
    }

    public void refresh() {
        catalog.refresh();
        reportScan();
    }

    private void reportScan() {
        if (catalog.directory() == null) {
            status.accept("No PROBLEMS directory set. Choose one in Settings.");
        } else if (catalog.lastError() != null) {
            status.accept(catalog.lastError());
        } else {
            status.accept(catalog.problems().size() + " problem(s) found");
        }
    }

    private Path toPath(String text) {
        try {
            return Path.of(text);
        } catch (RuntimeException e) {
            status.accept("The saved PROBLEMS setting is not a valid path: " + text);
            return null;
        }
    }
}
