package com.algoprep.studied;

import com.algoprep.csv.CsvLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * The last date each problem was studied, kept in a small CSV file ({@code key,date}) in the HOME
 * directory. Only the last date is kept, so marking a problem again overwrites it. Pure: no Swing.
 *
 * <p>Safety, because this is the first data AlgoPrep writes about the user's problems:
 * <ul>
 *   <li>Loading is tolerant. Blank lines and unreadable rows are skipped and reported, and a missing
 *       file just means no records.</li>
 *   <li>A file with rows but not one readable row is renamed to {@code .bad} before starting empty,
 *       so it is never overwritten.</li>
 *   <li>Every change re-reads the file first, so edits made outside AlgoPrep are not lost, and is
 *       written to a temporary file and renamed over the real one, so a crash cannot truncate it.</li>
 *   <li>Rows for problems that no longer exist are kept and written back unchanged.</li>
 * </ul>
 * Keys are matched case-insensitively, like the rest of AlgoPrep.
 */
public final class StudiedStore {

    public static final String FILE_NAME = "AlgoPrep_studied.csv";

    private static final String HEADER = "key,date";
    private static final String EOL = "\r\n";

    private record Entry(String key, LocalDate date) { }

    private final Clock clock;
    private final Consumer<String> status;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private Path file;
    private Map<String, Entry> entries = new HashMap<>();

    /**
     * @param clock  decides what "today" is, so tests can fix the date
     * @param status receives user-visible messages (skipped rows, a preserved bad file, a failed write)
     */
    public StudiedStore(Clock clock, Consumer<String> status) {
        this.clock = clock;
        this.status = status != null ? status : s -> { };
    }

    /** The CSV path for a HOME setting, or empty if HOME is unset or not a valid path. */
    public static Optional<Path> fileFor(String homeDir) {
        if (homeDir == null || homeDir.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Path.of(homeDir).resolve(FILE_NAME));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** Called after any change to the records, from whichever thread made the change. */
    public void addListener(Runnable listener) { listeners.add(listener); }

    /** Points the store at a file (or null for none) and loads it. The same path again does nothing. */
    public void setFile(Path newFile) {
        boolean changed;
        synchronized (this) {
            if (Objects.equals(file, newFile)) {
                return;
            }
            file = newFile;
            changed = replaceWith(newFile == null ? new HashMap<>() : read(newFile));
        }
        if (changed) {
            notifyListeners();
        }
    }

    public synchronized Optional<Path> file() { return Optional.ofNullable(file); }

    /** Re-reads the file, so edits made elsewhere appear. Notifies only if something changed. */
    public void reload() {
        boolean changed;
        synchronized (this) {
            if (file == null) {
                return;
            }
            changed = replaceWith(read(file));
        }
        if (changed) {
            notifyListeners();
        }
    }

    public synchronized Optional<LocalDate> studiedOn(String key) {
        return Optional.ofNullable(entries.get(lower(key))).map(Entry::date);
    }

    /**
     * Records today as the last studied date. Pressing it again the same day changes nothing.
     *
     * @return false if it could not be recorded (no file, unreadable file or failed write). The reason
     *         has been reported through the status callback and the old date is left in place.
     */
    public boolean markStudied(String key) {
        LocalDate today = LocalDate.now(clock);
        return modify(map -> {
            map.put(lower(key), new Entry(key, today));
            return map;
        });
    }

    /** Removes the record for a problem. Clearing a problem with no record is not an error. */
    public boolean clear(String key) {
        return modify(map -> {
            map.remove(lower(key));
            return map;
        });
    }

    // ---- internals ----

    private boolean modify(UnaryOperator<Map<String, Entry>> change) {
        boolean changed;
        synchronized (this) {
            if (file == null) {
                status.accept("Studied dates are not available. Select a HOME directory in Settings.");
                return false;
            }
            Map<String, Entry> onDisk = read(file);
            if (onDisk == null) {
                return false; // could not read it: do not risk overwriting what we cannot see
            }
            Map<String, Entry> next = change.apply(new HashMap<>(onDisk));
            if (!next.equals(onDisk) && !write(next)) {
                return false;
            }
            changed = replaceWith(next);
        }
        if (changed) {
            notifyListeners();
        }
        return true;
    }

    /** Adopts new records. A null result means "could not read", which keeps the current ones. */
    private boolean replaceWith(Map<String, Entry> next) {
        if (next == null || next.equals(entries)) {
            return false;
        }
        entries = next;
        return true;
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    /** Reads the CSV. Returns null if the file exists but could not be read. */
    private Map<String, Entry> read(Path path) {
        Map<String, Entry> result = new HashMap<>();
        if (!Files.exists(path)) {
            return result;
        }
        String text;
        try {
            // Malformed bytes become replacement characters instead of failing the whole load
            text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            status.accept("Could not read " + path.getFileName() + ": " + e.getMessage()
                    + ". Keeping the previous studied dates.");
            return null;
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }

        int dataRows = 0;
        int skipped = 0;
        for (String line : text.split("\r?\n", -1)) {
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = CsvLine.parse(line);
            if (isHeader(fields)) {
                continue;
            }
            dataRows++;
            Entry entry = toEntry(fields);
            if (entry == null) {
                skipped++;
            } else {
                result.put(lower(entry.key()), entry);
            }
        }

        if (dataRows > 0 && result.isEmpty()) {
            preserveBad(path);
            return new HashMap<>();
        }
        if (skipped > 0) {
            status.accept("Skipped " + skipped + " unreadable row(s) in " + path.getFileName()
                    + ". They are dropped the next time a date is saved.");
        }
        return result;
    }

    private static boolean isHeader(List<String> fields) {
        return fields != null && fields.size() == 2
                && fields.get(0).trim().equalsIgnoreCase("key")
                && fields.get(1).trim().equalsIgnoreCase("date");
    }

    private static Entry toEntry(List<String> fields) {
        if (fields == null || fields.size() != 2) {
            return null;
        }
        String key = fields.get(0).trim();
        if (key.isEmpty()) {
            return null;
        }
        try {
            return new Entry(key, LocalDate.parse(fields.get(1).trim()));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void preserveBad(Path path) {
        Path bad = path.resolveSibling(path.getFileName() + ".bad");
        try {
            Files.move(path, bad, StandardCopyOption.REPLACE_EXISTING);
            status.accept(path.getFileName() + " had no readable rows and was saved as "
                    + bad.getFileName() + ". Starting with no studied dates.");
        } catch (IOException e) {
            status.accept(path.getFileName() + " had no readable rows and could not be preserved ("
                    + e.getMessage() + "). Starting with no studied dates.");
        }
    }

    /** Writes sorted records to a temporary file, then renames it over the real file. */
    private boolean write(Map<String, Entry> map) {
        StringBuilder out = new StringBuilder(HEADER).append(EOL);
        map.values().stream()
                .sorted(Comparator.comparing((Entry e) -> e.key().toLowerCase(Locale.ROOT))
                        .thenComparing(Entry::key))
                .forEach(e -> out.append(quote(e.key())).append(',').append(e.date()).append(EOL));

        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, out, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // nothing more to do
            }
            status.accept("Could not save " + file.getFileName() + ": " + e.getMessage());
            return false;
        }
    }

    private static String quote(String key) {
        if (key.contains(",") || key.contains("\"") || key.contains("\n") || key.contains("\r")) {
            return '"' + key.replace("\"", "\"\"") + '"';
        }
        return key;
    }

    private static String lower(String key) {
        return key.toLowerCase(Locale.ROOT);
    }
}
