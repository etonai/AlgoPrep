package com.algoprep.studylist;

import com.algoprep.config.SettingsStore;
import com.algoprep.problem.ProblemCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudyListModelTest {

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private SettingsStore settings;
    private ProblemCatalog catalog;
    private Path problemsDir;

    @BeforeEach
    void setUp() throws Exception {
        settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        problemsDir = Files.createDirectories(tmp.resolve("problems"));
        addProblem("0001_two-sum");
        addProblem("0099_nodifficulty");
        catalog = new ProblemCatalog();
        catalog.setDirectory(problemsDir);
    }

    private void addProblem(String key) throws Exception {
        Files.writeString(problemsDir.resolve(key + "_problem.md"), "x");
    }

    private Path list(String name, String text) throws Exception {
        return Files.writeString(tmp.resolve(name), text);
    }

    private StudyListModel model() {
        return new StudyListModel(settings, catalog, status::add);
    }

    @Test
    void withNoSettingThereIsNoTabAndNoRows() {
        StudyListModel model = model();
        assertFalse(model.isActive());
        assertTrue(model.title().isEmpty());
        assertTrue(model.rows().isEmpty());
        assertNull(model.error());
        assertTrue(status.isEmpty());
    }

    @Test
    void aSavedSettingLoadsAtStartup() throws Exception {
        settings.setStudyListFile(list("Grind75.csv", "0001_two-sum, Easy, 20 minutes\n15_not-found, Tough\n").toString());

        StudyListModel model = model();

        assertTrue(model.isActive());
        assertEquals("Grind75", model.title().orElseThrow());
        assertEquals(2, model.rows().size());
        assertTrue(model.rows().get(0).isFound());
        assertFalse(model.rows().get(1).isFound());
    }

    @Test
    void followsTheSettingSetChangeAndClear() throws Exception {
        StudyListModel model = model();
        AtomicInteger notified = new AtomicInteger();
        model.addListener(notified::incrementAndGet);

        settings.setStudyListFile(list("A.csv", "0001_two-sum\n").toString());
        assertTrue(model.isActive());
        assertEquals("A", model.title().orElseThrow());
        assertEquals(1, model.rows().size());
        assertEquals(1, notified.get());

        settings.setStudyListFile(list("B.csv", "0001_two-sum\n0099_nodifficulty\n").toString());
        assertEquals("B", model.title().orElseThrow());
        assertEquals(2, model.rows().size());

        settings.setStudyListFile(null);
        assertFalse(model.isActive());
        assertTrue(model.rows().isEmpty());
    }

    @Test
    void unrelatedSettingChangesDoNotReloadOrNotify() throws Exception {
        settings.setStudyListFile(list("A.csv", "0001_two-sum\n").toString());
        StudyListModel model = model();
        AtomicInteger notified = new AtomicInteger();
        model.addListener(notified::incrementAndGet);

        settings.setHomeDir("C:\\somewhere");

        assertEquals(0, notified.get());
    }

    @Test
    void reResolvesWhenTheCatalogChanges() throws Exception {
        settings.setStudyListFile(list("A.csv", "0001_two-sum\n15_not-found\n").toString());
        StudyListModel model = model();
        assertFalse(model.rows().get(1).isFound());
        AtomicInteger notified = new AtomicInteger();
        model.addListener(notified::incrementAndGet);

        addProblem("0015_not-found");
        catalog.refresh();

        assertTrue(model.rows().get(1).isFound(), "the missing problem now has files");
        assertEquals(1, notified.get());
    }

    @Test
    void reloadPicksUpEditsToTheFileAndQuietReloadDoesNotRepeatMessages() throws Exception {
        Path file = list("A.csv", "0001_two-sum\n");
        settings.setStudyListFile(file.toString());
        StudyListModel model = model();

        Files.writeString(file, "0099_nodifficulty\n0001_two-sum\n\n,bad\n");
        model.reloadQuietly();
        assertEquals(2, model.rows().size());
        assertEquals(1, model.skipped());
        assertEquals("0099_nodifficulty", model.rows().get(0).entry().key(), "file order");
        assertTrue(status.isEmpty(), "quiet: no message");

        model.reload();
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("Skipped 1"), status.get(0));
    }

    @Test
    void reloadingAnUnchangedFileDoesNotNotify() throws Exception {
        settings.setStudyListFile(list("A.csv", "0001_two-sum\n").toString());
        StudyListModel model = model();
        AtomicInteger notified = new AtomicInteger();
        model.addListener(notified::incrementAndGet);

        model.reload();
        model.reloadQuietly();

        assertEquals(0, notified.get());
    }

    @Test
    void aMissingFileKeepsTheTabActiveWithAnErrorAndNoRows() {
        settings.setStudyListFile(tmp.resolve("gone.csv").toString());

        StudyListModel model = model();

        assertTrue(model.isActive(), "the tab still exists so the error can be shown");
        assertEquals("gone", model.title().orElseThrow());
        assertNotNull(model.error());
        assertTrue(model.rows().isEmpty());
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("not found"));
    }

    @Test
    void aFolderAndAnInvalidPathAreReportedNotThrown() throws Exception {
        settings.setStudyListFile(Files.createDirectory(tmp.resolve("folder.csv")).toString());
        StudyListModel model = model();
        assertNotNull(model.error());

        settings.setStudyListFile("bad\0path");
        assertNotNull(model.error());
        assertTrue(model.isActive());
    }

    @Test
    void theFileBecomingReadableLaterClearsTheError() throws Exception {
        Path file = tmp.resolve("later.csv");
        settings.setStudyListFile(file.toString());
        StudyListModel model = model();
        assertNotNull(model.error());

        Files.writeString(file, "0001_two-sum\n");
        model.reload();

        assertNull(model.error());
        assertEquals(1, model.rows().size());
    }

    @Test
    void theListFileIsNeverModified() throws Exception {
        Path file = list("A.csv", "0001_two-sum, Easy\r\n# note\r\n");
        byte[] before = Files.readAllBytes(file);
        settings.setStudyListFile(file.toString());
        StudyListModel model = model();
        model.reload();

        assertArrayEquals(before, Files.readAllBytes(file));
    }
}
