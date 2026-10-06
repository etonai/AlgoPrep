package com.algoprep.config;

import com.algoprep.theme.NativeTheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreTest {

    @TempDir
    Path dir;

    private final List<String> messages = new ArrayList<>();

    private SettingsStore open(Path file) {
        return new SettingsStore(file, messages::add);
    }

    @Test
    void missingFileGivesDefaults() {
        SettingsStore s = open(dir.resolve("settings.json"));
        assertEquals(NativeTheme.DARK, s.getTheme());
        assertNull(s.getInstructionsFile());
        assertNull(s.getProblemsDir());
        assertNull(s.getHomeDir());
        assertNull(s.getStagingRoot());
        assertNull(s.getLastSelectedKey());
        assertTrue(messages.isEmpty());
    }

    @Test
    void roundTripPersistsEveryField() {
        Path file = dir.resolve("sub").resolve("settings.json");
        SettingsStore s = open(file);
        s.setInstructionsFile("C:\\x\\instructions.md");
        s.setProblemsDir("C:\\problems");
        s.setHomeDir("C:\\home");
        s.setStagingRoot("C:\\staging");
        s.setLastSelectedKey("0001_two-sum");
        s.setTheme(NativeTheme.LIGHT);

        SettingsStore reloaded = open(file);
        assertEquals("C:\\x\\instructions.md", reloaded.getInstructionsFile());
        assertEquals("C:\\problems", reloaded.getProblemsDir());
        assertEquals("C:\\home", reloaded.getHomeDir());
        assertEquals("C:\\staging", reloaded.getStagingRoot());
        assertEquals("0001_two-sum", reloaded.getLastSelectedKey());
        assertEquals(NativeTheme.LIGHT, reloaded.getTheme());
        assertTrue(messages.isEmpty());
    }

    @Test
    void malformedFileIsPreservedAsBadAndDefaultsLoad() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{ this is not json");

        SettingsStore s = open(file);

        assertEquals(NativeTheme.DARK, s.getTheme());
        assertFalse(Files.exists(file));
        Path bad = dir.resolve("settings.json.bad");
        assertEquals("{ this is not json", Files.readString(bad));
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("settings.json.bad"));
    }

    @Test
    void emptyFileIsTreatedAsMalformed() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "");

        open(file);

        assertTrue(Files.exists(dir.resolve("settings.json.bad")));
        assertEquals(1, messages.size());
    }

    @Test
    void secondBadFileReplacesFirstBadFile() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(dir.resolve("settings.json.bad"), "old");
        Files.writeString(file, "{ nope");

        open(file);

        assertEquals("{ nope", Files.readString(dir.resolve("settings.json.bad")));
    }

    @Test
    void listenerFiresOnChangeButNotWhenValueIsUnchanged() {
        SettingsStore s = open(dir.resolve("settings.json"));
        AtomicInteger count = new AtomicInteger();
        s.addListener(count::incrementAndGet);

        s.setTheme(NativeTheme.LIGHT);
        assertEquals(1, count.get());

        s.setTheme(NativeTheme.LIGHT);
        assertEquals(1, count.get());

        s.setInstructionsFile("a.md");
        assertEquals(2, count.get());
    }

    @Test
    void unknownFieldsAndUnknownThemeAreTolerated() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, """
                { "instructionsFile": "i.md", "futureField": 42, "theme": "PURPLE" }
                """);

        SettingsStore s = open(file);

        assertEquals("i.md", s.getInstructionsFile());
        assertEquals(NativeTheme.DARK, s.getTheme());
        assertTrue(messages.isEmpty());
        assertFalse(Files.exists(dir.resolve("settings.json.bad")));
    }

    @Test
    void failedWriteIsReportedThroughStatusCallback() throws IOException {
        // The settings "file" path is an existing directory, so the write must fail.
        Path file = dir.resolve("settings.json");
        Files.createDirectory(file);
        SettingsStore s = open(file);
        messages.clear();

        s.setTheme(NativeTheme.LIGHT);

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).startsWith("Could not save settings"));
        assertEquals(NativeTheme.LIGHT, s.getTheme());
    }
}
