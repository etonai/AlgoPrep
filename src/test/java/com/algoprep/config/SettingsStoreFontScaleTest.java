package com.algoprep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreFontScaleTest {

    @TempDir
    Path dir;

    private final List<String> messages = new ArrayList<>();

    private SettingsStore open(Path file) {
        return new SettingsStore(file, messages::add);
    }

    @Test
    void unsetByDefault() {
        assertNull(open(dir.resolve("settings.json")).getFontScalePercent());
    }

    @Test
    void roundTrips() {
        Path file = dir.resolve("settings.json");

        open(file).setFontScalePercent(150);

        assertEquals(150, open(file).getFontScalePercent());
        assertTrue(messages.isEmpty());
    }

    @Test
    void anOldSettingsFileWithoutTheFieldStillLoads() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "{ \"instructionsFile\": \"i.md\", \"theme\": \"LIGHT\" }");

        SettingsStore s = open(file);

        assertEquals("i.md", s.getInstructionsFile());
        assertNull(s.getFontScalePercent());
        assertTrue(messages.isEmpty());
        assertFalse(Files.exists(dir.resolve("settings.json.bad")));
    }

    @Test
    void savingTheSizeKeepsTheOtherSettings() {
        Path file = dir.resolve("settings.json");
        SettingsStore s = open(file);
        s.setInstructionsFile("i.md");

        s.setFontScalePercent(80);

        SettingsStore reloaded = open(file);
        assertEquals("i.md", reloaded.getInstructionsFile());
        assertEquals(80, reloaded.getFontScalePercent());
    }

    @Test
    void listenerFiresOnlyWhenTheSizeChanges() {
        SettingsStore s = open(dir.resolve("settings.json"));
        int[] fired = {0};
        s.addListener(() -> fired[0]++);

        s.setFontScalePercent(120);
        s.setFontScalePercent(120);

        assertEquals(1, fired[0]);
    }
}
