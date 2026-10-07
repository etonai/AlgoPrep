package com.algoprep.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProblemScannerTest {

    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();

    private Path touch(String name) throws IOException {
        return Files.writeString(dir.resolve(name), "x");
    }

    private List<Problem> scan() throws IOException {
        return ProblemScanner.scan(dir, log::add);
    }

    private static List<String> keys(List<Problem> ps) {
        return ps.stream().map(Problem::key).toList();
    }

    @Test
    void statementOnlyProblemHasNoNotes() throws IOException {
        Path statement = touch("0001_two-sum_problem.md");

        List<Problem> ps = scan();

        assertEquals(1, ps.size());
        Problem p = ps.get(0);
        assertEquals("0001_two-sum", p.key());
        assertEquals(1, p.number());
        assertEquals("Two Sum", p.title());
        assertEquals(statement, p.statement());
        assertTrue(p.notes().isEmpty());
    }

    @Test
    void suppliedNotesAreFoundByFullKey() throws IOException {
        touch("0001_two-sum_problem.md");
        Path notes = touch("0001_two-sum_notes.md");

        assertEquals(notes, scan().get(0).notes().orElseThrow());
    }

    @Test
    void similarPrefixNamesStaySeparate() throws IOException {
        touch("0001_two-sum_problem.md");
        touch("0001_two-sum-ii_problem.md");
        Path notesForIi = touch("0001_two-sum-ii_notes.md");

        List<Problem> ps = scan();

        assertEquals(List.of("0001_two-sum", "0001_two-sum-ii"), keys(ps));
        assertTrue(ps.get(0).notes().isEmpty(), "notes of two-sum-ii must not join two-sum");
        assertEquals(notesForIi, ps.get(1).notes().orElseThrow());
    }

    @Test
    void notesMustMatchTheFullKeyNotTheNumber() throws IOException {
        touch("0001_two-sum_problem.md");
        touch("0001_other_notes.md");
        touch("0001_notes.md");

        assertTrue(scan().get(0).notes().isEmpty());
    }

    @Test
    void duplicateNumbersWithDifferentSlugsAreSeparateRows() throws IOException {
        touch("0007_beta_problem.md");
        touch("0007_alpha_problem.md");

        assertEquals(List.of("0007_alpha", "0007_beta"), keys(scan()));
    }

    @Test
    void namesAreMatchedCaseInsensitivelyAndRealNamesKept() throws IOException {
        Path statement = touch("0002_Add-Two_PROBLEM.MD");
        Path notes = touch("0002_add-two_Notes.Md");

        Problem p = scan().get(0);

        assertEquals("0002_Add-Two", p.key());
        assertEquals(statement, p.statement());
        assertEquals(notes, p.notes().orElseThrow());
    }

    @Test
    void sortIsNumericNotTextual() throws IOException {
        touch("0010_ten_problem.md");
        touch("0002_two_problem.md");
        touch("0100_hundred_problem.md");
        touch("1_one_problem.md");

        assertEquals(List.of("1_one", "0002_two", "0010_ten", "0100_hundred"), keys(scan()));
    }

    @Test
    void solutionsTestCasesAndUnrelatedFilesAreIgnored() throws IOException {
        touch("0001_two-sum_problem.md");
        touch("0001_two-sum_solution.java");
        touch("0001_two-sum_testcases.md");
        touch("readme.txt");
        touch("0002_orphan_notes.md");

        List<Problem> ps = scan();

        assertEquals(List.of("0001_two-sum"), keys(ps));
        assertTrue(log.isEmpty(), "unrelated files are not even logged: " + log);
    }

    @Test
    void subdirectoriesAreNotScanned() throws IOException {
        Path sub = Files.createDirectory(dir.resolve("nested"));
        Files.writeString(sub.resolve("0001_hidden_problem.md"), "x");
        Files.createDirectory(dir.resolve("0002_dir_problem.md"));

        assertTrue(scan().isEmpty());
    }

    @Test
    void statementLikeNamesThatDoNotMatchAreLoggedNotListed() throws IOException {
        touch("two-sum_problem.md");          // no number
        touch("0003_x_problem.txt");          // wrong extension
        touch("0004_y_problem.md.bak");       // wrong ending
        touch("0005_ok_problem.md");

        List<Problem> ps = scan();

        assertEquals(List.of("0005_ok"), keys(ps));
        assertEquals(3, log.size(), log.toString());
        assertTrue(log.stream().anyMatch(m -> m.contains("two-sum_problem.md")));
    }

    @Test
    void emptyDirectoryGivesEmptyList() throws IOException {
        assertTrue(scan().isEmpty());
    }

    @Test
    void missingDirectoryThrowsRatherThanReturningEmpty() {
        assertThrows(NoSuchFileException.class,
                () -> ProblemScanner.scan(dir.resolve("nope"), log::add));
    }

    @Test
    void pathThatIsAFileThrows() throws IOException {
        Path file = touch("a-file.txt");
        assertThrows(NotDirectoryException.class, () -> ProblemScanner.scan(file, log::add));
    }

    @Test
    void returnedListIsImmutable() throws IOException {
        touch("0001_a_problem.md");
        assertThrows(UnsupportedOperationException.class, () -> scan().clear());
    }
}
