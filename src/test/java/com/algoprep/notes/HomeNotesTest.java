package com.algoprep.notes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HomeNotesTest {

    @TempDir
    Path home;

    @Test
    void fileNameIsKeyPlusSuffix() {
        assertEquals("0001_two-sum_AlgoPrepNotes.md", HomeNotes.fileName("0001_two-sum"));
    }

    @Test
    void findsTheNotesFile() throws IOException {
        Path notes = Files.writeString(home.resolve("0001_two-sum_AlgoPrepNotes.md"), "n");

        assertEquals(notes, HomeNotes.find(home, "0001_two-sum").orElseThrow());
    }

    @Test
    void missingFileIsEmpty() throws IOException {
        Files.writeString(home.resolve("0002_other_AlgoPrepNotes.md"), "n");

        assertTrue(HomeNotes.find(home, "0001_two-sum").isEmpty());
    }

    @Test
    void matchesCaseInsensitivelyAndKeepsTheRealName() throws IOException {
        Path notes = Files.writeString(home.resolve("0001_Two-Sum_ALGOPREPNOTES.MD"), "n");

        assertEquals(notes, HomeNotes.find(home, "0001_two-sum").orElseThrow());
    }

    @Test
    void onlyTheFullKeyMatches() throws IOException {
        Files.writeString(home.resolve("0001_two-sum-ii_AlgoPrepNotes.md"), "n");
        Files.writeString(home.resolve("0001_AlgoPrepNotes.md"), "n");

        assertTrue(HomeNotes.find(home, "0001_two-sum").isEmpty());
    }

    @Test
    void aDirectoryWithThatNameIsNotANotesFile() throws IOException {
        Files.createDirectory(home.resolve("0001_two-sum_AlgoPrepNotes.md"));

        assertTrue(HomeNotes.find(home, "0001_two-sum").isEmpty());
    }

    @Test
    void ordinaryNotesAndOtherFilesAreNotPersonalNotes() throws IOException {
        Files.writeString(home.resolve("0001_two-sum_notes.md"), "n");
        Files.writeString(home.resolve("0001_two-sum_solution.java"), "n");

        assertTrue(HomeNotes.find(home, "0001_two-sum").isEmpty());
    }

    @Test
    void unlistableDirectoryThrows() {
        assertThrows(NoSuchFileException.class, () -> HomeNotes.find(home.resolve("nope"), "0001_x"));
    }
}
