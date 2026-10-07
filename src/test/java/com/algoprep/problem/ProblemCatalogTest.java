package com.algoprep.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProblemCatalogTest {

    @TempDir
    Path dir;

    private void problem(String key) throws IOException {
        Files.writeString(dir.resolve(key + "_problem.md"), "x");
    }

    @Test
    void startsEmptyWithNoError() {
        ProblemCatalog c = new ProblemCatalog();
        assertNull(c.directory());
        assertTrue(c.problems().isEmpty());
        assertNull(c.lastError());
    }

    @Test
    void setDirectoryScansAndNotifies() throws IOException {
        problem("0001_a");
        ProblemCatalog c = new ProblemCatalog();
        AtomicInteger fired = new AtomicInteger();
        c.addListener(fired::incrementAndGet);

        c.setDirectory(dir);

        assertEquals(1, c.problems().size());
        assertNull(c.lastError());
        assertEquals(1, fired.get());
    }

    @Test
    void refreshPicksUpNewFiles() throws IOException {
        problem("0001_a");
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(dir);

        problem("0002_b");
        c.refresh();

        assertEquals(2, c.problems().size());
    }

    @Test
    void failedRefreshKeepsThePreviousListAndRecordsTheError() throws IOException {
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.writeString(sub.resolve("0001_a_problem.md"), "x");
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(sub);
        assertEquals(1, c.problems().size());

        Files.delete(sub.resolve("0001_a_problem.md"));
        Files.delete(sub);
        c.refresh();

        assertEquals(1, c.problems().size(), "previous list is kept");
        assertTrue(c.lastError().contains("not found"));
    }

    @Test
    void successfulRefreshClearsTheError() throws IOException {
        Path sub = dir.resolve("sub");
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(sub);
        assertNotNull(c.lastError());

        Files.createDirectory(sub);
        Files.writeString(sub.resolve("0001_a_problem.md"), "x");
        c.refresh();

        assertNull(c.lastError());
        assertEquals(1, c.problems().size());
    }

    @Test
    void badNewDirectoryDoesNotShowTheOldDirectorysProblems() throws IOException {
        problem("0001_a");
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(dir);
        assertEquals(1, c.problems().size());

        c.setDirectory(dir.resolve("missing"));

        assertTrue(c.problems().isEmpty());
        assertNotNull(c.lastError());
    }

    @Test
    void pathThatIsAFileReportsNotADirectory() throws IOException {
        Path file = Files.writeString(dir.resolve("f.txt"), "x");
        ProblemCatalog c = new ProblemCatalog();

        c.setDirectory(file);

        assertTrue(c.lastError().contains("not a directory"));
    }

    @Test
    void unsetDirectoryIsEmptyWithoutError() {
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(null);
        c.refresh();
        assertTrue(c.problems().isEmpty());
        assertNull(c.lastError());
    }

    @Test
    void emptyDirectoryIsNotAnError() {
        ProblemCatalog c = new ProblemCatalog();
        c.setDirectory(dir);
        assertTrue(c.problems().isEmpty());
        assertNull(c.lastError());
    }
}
