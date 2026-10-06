package com.chatstory.context;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ContextFileStore {

    private final Path jsonFilePath;
    private final Path defaultStagingPath;
    private Path currentStagingPath;
    private Path lastDirectory;
    private final List<Path> entries = new ArrayList<>();
    private final Set<Path> checkedFiles = new LinkedHashSet<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<Runnable> stagingPathListeners = new ArrayList<>();
    private final List<Runnable> checkedListeners = new ArrayList<>();
    private final Gson gson = new Gson();

    public ContextFileStore(String jsonFilePath, String defaultStagingFolderPath) {
        this.jsonFilePath = Path.of(jsonFilePath);
        this.defaultStagingPath = Path.of(defaultStagingFolderPath).toAbsolutePath();
        this.currentStagingPath = this.defaultStagingPath;
        load();
    }

    private void load() {
        if (!Files.exists(jsonFilePath)) return;
        try (Reader reader = Files.newBufferedReader(jsonFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data == null) return;
            if (data.files != null) {
                for (String p : data.files) {
                    Path absolute = Path.of(p).toAbsolutePath();
                    if (!entries.contains(absolute)) entries.add(absolute);
                }
            }
            if (data.lastDirectory != null) {
                lastDirectory = Path.of(data.lastDirectory);
            }
            if (data.stagingPath != null) {
                currentStagingPath = Path.of(data.stagingPath).toAbsolutePath();
            }
            if (data.checkedFiles != null) {
                for (String p : data.checkedFiles) {
                    checkedFiles.add(Path.of(p).toAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("[ContextFileStore] Failed to load file list: " + e.getMessage()
                    + " — starting with empty list");
        }
    }

    public void add(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        if (entries.contains(absolute)) return;
        entries.add(absolute);
        save();
        for (Runnable listener : listeners) listener.run();
    }

    public void remove(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        boolean removed = entries.remove(absolute);
        checkedFiles.remove(absolute);
        if (removed) {
            save();
            for (Runnable listener : listeners) listener.run();
            for (Runnable listener : checkedListeners) listener.run();
        }
    }

    public boolean isChecked(Path filePath) {
        return checkedFiles.contains(filePath.toAbsolutePath());
    }

    public void setChecked(Path filePath, boolean checked) {
        Path absolute = filePath.toAbsolutePath();
        if (checked) {
            checkedFiles.add(absolute);
        } else {
            checkedFiles.remove(absolute);
        }
        save();
        for (Runnable listener : checkedListeners) listener.run();
    }

    public List<Path> getCheckedEntries() {
        List<Path> result = new ArrayList<>();
        for (Path entry : entries) {
            if (checkedFiles.contains(entry)) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    public void addCheckedListener(Runnable listener) {
        checkedListeners.add(listener);
    }

    public List<Path> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void replaceAll(List<Path> files, List<Path> checked, Path lastDirectory, Path stagingPath) {
        entries.clear();
        checkedFiles.clear();

        if (files != null) {
            for (Path file : files) {
                if (file == null) continue;
                Path absolute = file.toAbsolutePath();
                if (!entries.contains(absolute)) entries.add(absolute);
            }
        }

        if (checked != null) {
            for (Path file : checked) {
                if (file == null) continue;
                Path absolute = file.toAbsolutePath();
                if (entries.contains(absolute)) checkedFiles.add(absolute);
            }
        }

        this.lastDirectory = lastDirectory;
        this.currentStagingPath = stagingPath != null
                ? stagingPath.toAbsolutePath()
                : defaultStagingPath;

        save();
        for (Runnable listener : listeners) listener.run();
        for (Runnable listener : checkedListeners) listener.run();
        for (Runnable listener : stagingPathListeners) listener.run();
    }

    public Path getLastDirectory() {
        return lastDirectory;
    }

    public void setLastDirectory(Path dir) {
        this.lastDirectory = dir;
        save();
    }

    public Path getStagingPath() {
        return currentStagingPath;
    }

    public void setStagingPath(Path path) {
        this.currentStagingPath = path.toAbsolutePath();
        save();
        for (Runnable listener : stagingPathListeners) listener.run();
    }

    public void addStagingPathListener(Runnable listener) {
        stagingPathListeners.add(listener);
    }

    public void clearStaging() {
        if (!Files.exists(currentStagingPath)) return;
        try (var stream = Files.list(currentStagingPath)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    System.err.println("[ContextFileStore] Could not delete staged file: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("[ContextFileStore] Could not list staging directory for clear: " + e.getMessage());
        }
    }

    public StagingResult stageSelected(List<Path> selected) {
        try {
            Files.createDirectories(currentStagingPath);
        } catch (IOException e) {
            System.err.println("[ContextFileStore] Could not create staging folder: " + e.getMessage());
            return new StagingResult(0, selected.size(),
                    List.of("Could not create staging folder: " + e.getMessage()));
        }

        int succeeded = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Path source : selected) {
            Path target = currentStagingPath.resolve(source.getFileName());
            boolean overwriting = Files.exists(target);
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                if (overwriting) {
                    System.out.println("[ContextFileStore] Overwrote existing staged file: "
                            + source.getFileName());
                }
                succeeded++;
            } catch (IOException e) {
                failed++;
                String msg = source.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                System.err.println("[ContextFileStore] Stage failed — " + msg);
            }
        }

        return new StagingResult(succeeded, failed, Collections.unmodifiableList(failures));
    }

    private void save() {
        StoredData data = new StoredData();
        for (Path p : entries) data.files.add(p.toString());
        for (Path p : checkedFiles) data.checkedFiles.add(p.toString());
        if (lastDirectory != null) data.lastDirectory = lastDirectory.toString();
        if (!currentStagingPath.equals(defaultStagingPath)) {
            data.stagingPath = currentStagingPath.toString();
        }
        try (Writer writer = Files.newBufferedWriter(jsonFilePath, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[ContextFileStore] Failed to save file list: " + e.getMessage());
        }
    }

    public record StagingResult(int succeeded, int failed, List<String> failureMessages) {}

    private static class StoredData {
        List<String> files = new ArrayList<>();
        List<String> checkedFiles = new ArrayList<>();
        String lastDirectory;
        String stagingPath;
    }
}
