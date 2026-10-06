package com.algoprep.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @TempDir
    Path dir;

    private String p(String... parts) {
        Path p = dir;
        for (String part : parts) {
            p = p.resolve(part);
        }
        return p.toString();
    }

    @Test
    void derivesPathsFromLocalAndRoamingFolders() {
        AppConfig c = new AppConfig(p("local"), p("roam"), p("home"));

        assertEquals(p("local", "AlgoPrep", "profile"), c.getProfilePath());
        assertEquals(p("local", "AlgoPrep", "upload-staging"), c.getDefaultStagingRootPath());
        assertEquals(p("roam", "AlgoPrep", "settings.json"), c.getSettingsFilePath());
        assertEquals(p("roam", "AlgoPrep", "config.properties"), c.getConfigFilePath());
        assertTrue(Files.isDirectory(Path.of(c.getProfilePath())));
        assertTrue(Files.isDirectory(Path.of(p("roam", "AlgoPrep"))));
    }

    @Test
    void blankOrNullBaseFoldersFallBackToUserHome() {
        AppConfig c = new AppConfig(null, "  ", p("home"));

        assertEquals(p("home", "AlgoPrep", "profile"), c.getProfilePath());
        assertEquals(p("home", "AlgoPrep", "settings.json"), c.getSettingsFilePath());
    }

    @Test
    void defaultsToChatGptUrlWhenNoConfigFile() {
        assertEquals("https://chatgpt.com",
                new AppConfig(p("local"), p("roam"), p("home")).getTargetUrl());
    }

    @Test
    void readsTargetUrlFromConfigFile() throws IOException {
        Path roam = Files.createDirectories(dir.resolve("roam").resolve("AlgoPrep"));
        Files.writeString(roam.resolve("config.properties"), "target.chat.url= https://example.test/c/1 \n");

        assertEquals("https://example.test/c/1",
                new AppConfig(p("local"), p("roam"), p("home")).getTargetUrl());
    }

    @Test
    void blankUrlOrMalformedConfigNeverPreventsStartup() throws IOException {
        Path roam = Files.createDirectories(dir.resolve("roam").resolve("AlgoPrep"));
        Path cfg = roam.resolve("config.properties");

        Files.writeString(cfg, "target.chat.url=\n");
        assertEquals("https://chatgpt.com", new AppConfig(p("local"), p("roam"), p("home")).getTargetUrl());

        Files.writeString(cfg, "target.chat.url=\\u12\n");
        assertEquals("https://chatgpt.com", new AppConfig(p("local"), p("roam"), p("home")).getTargetUrl());
    }
}
