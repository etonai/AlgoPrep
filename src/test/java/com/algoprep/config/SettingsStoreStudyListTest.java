package com.algoprep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SettingsStoreStudyListTest {

    @TempDir
    Path tmp;

    @Test
    void roundTripsAndDefaultsToNull() {
        Path file = tmp.resolve("settings.json");
        SettingsStore store = new SettingsStore(file, m -> { });
        assertNull(store.getStudyListFile());

        store.setStudyListFile("C:\\lists\\Grind75.csv");

        assertEquals("C:\\lists\\Grind75.csv", new SettingsStore(file, m -> { }).getStudyListFile());
    }

    @Test
    void settingAndClearingNotifyOnceEachAndOnlyOnRealChange() {
        SettingsStore store = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        AtomicInteger notified = new AtomicInteger();
        store.addListener(notified::incrementAndGet);

        store.setStudyListFile("a.csv");
        store.setStudyListFile("a.csv");
        assertEquals(1, notified.get());

        store.setStudyListFile(null);
        assertEquals(2, notified.get());
        assertNull(store.getStudyListFile());
    }

    @Test
    void anOldSettingsFileWithoutTheFieldStillLoads() throws Exception {
        Path file = tmp.resolve("settings.json");
        Files.writeString(file, "{\"homeDir\":\"C:\\\\home\",\"theme\":\"LIGHT\"}");

        SettingsStore store = new SettingsStore(file, m -> fail("no message expected: " + m));

        assertEquals("C:\\home", store.getHomeDir());
        assertNull(store.getStudyListFile());
    }
}
