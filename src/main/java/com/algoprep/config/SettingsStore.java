package com.algoprep.config;

import com.algoprep.theme.NativeTheme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Single Gson JSON file holding all AlgoPrep settings. Loads tolerantly and saves on every change.
 * Fields beyond the instructions file and theme are reserved for later cycles so the file format
 * does not change (Plan section 10).
 */
public final class SettingsStore {

    /** Serialized form. Unknown fields in the file are ignored by Gson. */
    private static final class Data {
        String instructionsFile;
        String problemsDir;
        String homeDir;
        String stagingRoot;
        NativeTheme theme;
        String lastSelectedKey;
        Integer fontScalePercent;
        String studyListFile;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Consumer<String> status;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private Data data;

    /**
     * @param file   the settings.json path
     * @param status receives user-visible messages (parse failure, write failure). It may be
     *               called from this constructor, so it must not depend on UI built afterwards.
     */
    public SettingsStore(Path file, Consumer<String> status) {
        this.file = file;
        this.status = status != null ? status : s -> { };
        this.data = load();
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    // ---- accessors (null means "not set", except theme which defaults to DARK) ----

    public synchronized String getInstructionsFile() { return data.instructionsFile; }
    public synchronized String getProblemsDir()      { return data.problemsDir; }
    public synchronized String getHomeDir()          { return data.homeDir; }
    public synchronized String getStagingRoot()      { return data.stagingRoot; }
    public synchronized String getLastSelectedKey()  { return data.lastSelectedKey; }
    /** The optional study list CSV file, or null if none is set. */
    public synchronized String getStudyListFile()    { return data.studyListFile; }
    /** The saved display text size in percent, or null if none is saved. */
    public synchronized Integer getFontScalePercent() { return data.fontScalePercent; }
    public synchronized NativeTheme getTheme()       { return data.theme != null ? data.theme : NativeTheme.DARK; }

    /** The saved staging root, or {@code defaultRoot} when none is saved. */
    public synchronized String effectiveStagingRoot(String defaultRoot) {
        return data.stagingRoot != null && !data.stagingRoot.isBlank() ? data.stagingRoot : defaultRoot;
    }

    public void setInstructionsFile(String v) { update(() -> data.instructionsFile = v, () -> data.instructionsFile); }
    public void setProblemsDir(String v)      { update(() -> data.problemsDir = v,      () -> data.problemsDir); }
    public void setHomeDir(String v)          { update(() -> data.homeDir = v,          () -> data.homeDir); }
    public void setStagingRoot(String v)      { update(() -> data.stagingRoot = v,      () -> data.stagingRoot); }
    public void setLastSelectedKey(String v)  { update(() -> data.lastSelectedKey = v,  () -> data.lastSelectedKey); }
    public void setStudyListFile(String v)    { update(() -> data.studyListFile = v,    () -> data.studyListFile); }
    public void setFontScalePercent(int v)    { update(() -> data.fontScalePercent = v,   () -> data.fontScalePercent); }
    public void setTheme(NativeTheme v)       { update(() -> data.theme = v,            () -> data.theme); }

    // ---- internals ----

    /** Applies a change, and saves and notifies only if the value actually changed. */
    private void update(Runnable change, java.util.function.Supplier<Object> current) {
        synchronized (this) {
            Object before = current.get();
            change.run();
            if (java.util.Objects.equals(before, current.get())) {
                return;
            }
            save();
        }
        listeners.forEach(Runnable::run);
    }

    private Data load() {
        if (!Files.exists(file)) {
            return new Data();
        }
        try {
            Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (loaded == null) {
                throw new JsonParseException("settings file is empty");
            }
            return loaded;
        } catch (JsonParseException e) {
            preserveBadFile();
        } catch (IOException e) {
            status.accept("Could not read settings (" + e.getMessage() + "). Using defaults.");
        }
        return new Data();
    }

    private void preserveBadFile() {
        Path bad = file.resolveSibling(file.getFileName() + ".bad");
        try {
            Files.move(file, bad, StandardCopyOption.REPLACE_EXISTING);
            status.accept("Settings file was unreadable and was saved as " + bad.getFileName()
                    + ". Using defaults.");
        } catch (IOException e) {
            status.accept("Settings file was unreadable and could not be preserved ("
                    + e.getMessage() + "). Using defaults.");
        }
    }

    private void save() {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            status.accept("Could not save settings: " + e.getMessage());
        }
    }
}
