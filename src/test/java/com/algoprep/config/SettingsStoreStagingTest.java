package com.algoprep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsStoreStagingTest {

    @TempDir
    Path dir;

    @Test
    void effectiveStagingRootFallsBackToTheDefaultUntilOneIsSaved() {
        SettingsStore s = new SettingsStore(dir.resolve("settings.json"), m -> { });

        assertEquals("C:\\default", s.effectiveStagingRoot("C:\\default"));

        s.setStagingRoot("C:\\mine");
        assertEquals("C:\\mine", s.effectiveStagingRoot("C:\\default"));

        s.setStagingRoot(null);
        assertEquals("C:\\default", s.effectiveStagingRoot("C:\\default"));

        s.setStagingRoot("   ");
        assertEquals("C:\\default", s.effectiveStagingRoot("C:\\default"));
    }
}
