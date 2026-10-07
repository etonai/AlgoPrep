package com.algoprep.problem;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the current problem list and the last scan error. Separate from the scanner and the
 * Swing panels so the "keep the previous list when a scan fails" rule (Plan section 5.3) can be
 * unit tested.
 *
 * <p>{@link #setDirectory} starts from an empty list, so a bad new directory never shows the old
 * directory's problems. {@link #refresh} rescans the same directory and keeps the previous list if
 * the scan fails.
 */
public final class ProblemCatalog {

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private Path directory;
    private List<Problem> problems = List.of();
    private String lastError;

    public void addListener(Runnable listener) { listeners.add(listener); }

    public synchronized Path directory()        { return directory; }
    public synchronized List<Problem> problems() { return problems; }
    /** The message from the last scan, or null if it succeeded (or nothing was scanned). */
    public synchronized String lastError()      { return lastError; }

    /** Switches directory (null means unset) and scans it. */
    public void setDirectory(Path newDirectory) {
        synchronized (this) {
            directory = newDirectory;
            problems = List.of();
            lastError = null;
            scanLocked();
        }
        notifyListeners();
    }

    /** Rescans the current directory. A failed scan keeps the previous list and records the error. */
    public void refresh() {
        synchronized (this) {
            scanLocked();
        }
        notifyListeners();
    }

    private void scanLocked() {
        if (directory == null) {
            return;
        }
        try {
            problems = ProblemScanner.scan(directory);
            lastError = null;
        } catch (NoSuchFileException e) {
            lastError = "PROBLEMS directory not found: " + directory;
        } catch (NotDirectoryException e) {
            lastError = "PROBLEMS path is not a directory: " + directory;
        } catch (AccessDeniedException e) {
            lastError = "PROBLEMS directory cannot be read: " + directory;
        } catch (IOException | RuntimeException e) {
            lastError = "Could not scan PROBLEMS directory: " + e.getMessage();
        }
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
