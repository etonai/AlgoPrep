package com.algoprep.display;

import com.algoprep.problem.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DocumentLoaderTest {

    @TempDir
    Path tmp;

    private Path problems;
    private Path home;

    private Path write(Path dir, String name, String content) throws IOException {
        return Files.writeString(dir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private void dirs() throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        home = Files.createDirectories(tmp.resolve("home"));
    }

    private Problem problem(String key, Path statement, Path notes) {
        return new Problem(key, 1, "Two Sum", statement, Optional.ofNullable(notes));
    }

    // ---- problem and notes tabs ----

    @Test
    void statementAndSuppliedNotesAreRead() throws IOException {
        dirs();
        Problem p = problem("0001_two-sum",
                write(problems, "0001_two-sum_problem.md", "# Two Sum"),
                write(problems, "0001_two-sum_notes.md", "a hint"));

        assertEquals("# Two Sum", DocumentLoader.problem(Optional.of(p), Optional.of(p.key())).markdown());
        assertEquals("a hint", DocumentLoader.notes(Optional.of(p), Optional.of(p.key())).markdown());
    }

    @Test
    void nothingSelected() {
        DocumentLoader.Doc problem = DocumentLoader.problem(Optional.empty(), Optional.empty());
        DocumentLoader.Doc notes = DocumentLoader.notes(Optional.empty(), Optional.empty());
        DocumentLoader.Doc mine = DocumentLoader.myNotes(Optional.empty(), "C:\\home");

        assertEquals(DocumentLoader.NOTHING_SELECTED, problem.message());
        assertEquals(DocumentLoader.NOTHING_SELECTED, notes.message());
        assertEquals(DocumentLoader.NOTHING_SELECTED, mine.message());
    }

    @Test
    void unavailableProblemShowsRefreshMessageOnProblemAndNotes() {
        Optional<String> key = Optional.of("0001_two-sum");

        assertEquals(DocumentLoader.UNAVAILABLE, DocumentLoader.problem(Optional.empty(), key).message());
        assertEquals(DocumentLoader.UNAVAILABLE, DocumentLoader.notes(Optional.empty(), key).message());
    }

    @Test
    void missingSuppliedNotes() throws IOException {
        dirs();
        Problem p = problem("0001_two-sum", write(problems, "0001_two-sum_problem.md", "x"), null);

        assertEquals(DocumentLoader.NO_SUPPLIED_NOTES,
                DocumentLoader.notes(Optional.of(p), Optional.of(p.key())).message());
    }

    @Test
    void notesDeletedSinceTheScanCountAsMissingNotAsAnError() throws IOException {
        dirs();
        Path notes = write(problems, "0001_two-sum_notes.md", "x");
        Problem p = problem("0001_two-sum", write(problems, "0001_two-sum_problem.md", "x"), notes);
        Files.delete(notes);

        assertEquals(DocumentLoader.NO_SUPPLIED_NOTES,
                DocumentLoader.notes(Optional.of(p), Optional.of(p.key())).message());
    }

    @Test
    void statementDeletedSinceTheScanGivesAReadMessage() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md", "x");
        Problem p = problem("0001_two-sum", statement, null);
        Files.delete(statement);

        DocumentLoader.Doc doc = DocumentLoader.problem(Optional.of(p), Optional.of(p.key()));

        assertFalse(doc.isText());
        assertTrue(doc.message().startsWith("Could not read 0001_two-sum_problem.md"), doc.message());
    }

    // ---- my notes tab ----

    @Test
    void homeUnset() {
        Optional<String> key = Optional.of("0001_two-sum");

        assertEquals(DocumentLoader.HOME_UNSET, DocumentLoader.myNotes(key, null).message());
        assertEquals(DocumentLoader.HOME_UNSET, DocumentLoader.myNotes(key, "  ").message());
    }

    @Test
    void homeNotFound() {
        DocumentLoader.Doc doc = DocumentLoader.myNotes(Optional.of("0001_two-sum"),
                tmp.resolve("missing").toString());

        assertTrue(doc.message().startsWith("HOME directory not found: "), doc.message());
    }

    @Test
    void homeThatIsAFileIsNotFound() throws IOException {
        Path file = write(tmp, "file.txt", "x");

        assertTrue(DocumentLoader.myNotes(Optional.of("0001_two-sum"), file.toString())
                .message().startsWith("HOME directory not found"));
    }

    @Test
    void noSavedNotes() throws IOException {
        dirs();

        assertEquals(DocumentLoader.NO_SAVED_NOTES,
                DocumentLoader.myNotes(Optional.of("0001_two-sum"), home.toString()).message());
    }

    @Test
    void savedNotesAreReadByFullKeyAndCaseInsensitively() throws IOException {
        dirs();
        write(home, "0001_Two-Sum_ALGOPREPNOTES.md", "mine");
        write(home, "0001_two-sum-ii_AlgoPrepNotes.md", "wrong one");

        DocumentLoader.Doc doc = DocumentLoader.myNotes(Optional.of("0001_two-sum"), home.toString());

        assertEquals("mine", doc.markdown());
    }

    @Test
    void notesForASimilarKeyAreNotShown() throws IOException {
        dirs();
        write(home, "0001_two-sum-ii_AlgoPrepNotes.md", "wrong one");

        assertEquals(DocumentLoader.NO_SAVED_NOTES,
                DocumentLoader.myNotes(Optional.of("0001_two-sum"), home.toString()).message());
    }

    @Test
    void savedNotesStillLoadWhenTheStatementIsUnavailable() throws IOException {
        dirs();
        write(home, "0001_two-sum_AlgoPrepNotes.md", "mine");

        assertEquals("mine", DocumentLoader.myNotes(Optional.of("0001_two-sum"), home.toString()).markdown());
    }

    // ---- reading ----

    @Test
    void invalidUtf8IsReplacedNotRejected() throws IOException {
        dirs();
        Path statement = problems.resolve("0001_a_problem.md");
        Files.write(statement, new byte[] {'o', 'k', ' ', (byte) 0xC3, (byte) 0x28});
        Problem p = problem("0001_a", statement, null);

        DocumentLoader.Doc doc = DocumentLoader.problem(Optional.of(p), Optional.of(p.key()));

        assertTrue(doc.isText());
        assertTrue(doc.markdown().startsWith("ok "));
    }

    @Test
    void byteOrderMarkIsDropped() throws IOException {
        dirs();
        Path statement = write(problems, "0001_a_problem.md", "\uFEFF# Title");
        Problem p = problem("0001_a", statement, null);

        assertEquals("# Title", DocumentLoader.problem(Optional.of(p), Optional.of(p.key())).markdown());
    }

    @Test
    void emptyFileGivesAMessage() throws IOException {
        dirs();
        Path statement = write(problems, "0001_a_problem.md", "  \n");
        Problem p = problem("0001_a", statement, null);

        DocumentLoader.Doc doc = DocumentLoader.problem(Optional.of(p), Optional.of(p.key()));

        assertEquals("0001_a_problem.md is empty.", doc.message());
    }

    @Test
    void oversizedFileIsNotReadAndGivesAMessage() throws IOException {
        dirs();
        Path statement = problems.resolve("0001_a_problem.md");
        Files.write(statement, new byte[(int) DocumentLoader.MAX_BYTES + 1]);
        Problem p = problem("0001_a", statement, null);

        DocumentLoader.Doc doc = DocumentLoader.problem(Optional.of(p), Optional.of(p.key()));

        assertFalse(doc.isText());
        assertTrue(doc.message().contains("too large to display"), doc.message());
    }

    @Test
    void fileAtTheLimitIsStillShown() throws IOException {
        dirs();
        Path statement = problems.resolve("0001_a_problem.md");
        Files.write(statement, "a".repeat((int) DocumentLoader.MAX_BYTES).getBytes(StandardCharsets.UTF_8));
        Problem p = problem("0001_a", statement, null);

        assertTrue(DocumentLoader.problem(Optional.of(p), Optional.of(p.key())).isText());
    }

    @Test
    void solutionAndTestCaseFilesNextToTheStatementAreNeverRead() throws IOException {
        dirs();
        Path statement = write(problems, "0001_a_problem.md", "statement");
        Path solution = write(problems, "0001_a_solution.java", "SECRET SOLUTION");
        Path cases = write(problems, "0001_a_testcases.md", "SECRET CASES");
        Problem p = problem("0001_a", statement, null);

        DocumentLoader.Doc problemDoc = DocumentLoader.problem(Optional.of(p), Optional.of(p.key()));
        DocumentLoader.Doc notesDoc = DocumentLoader.notes(Optional.of(p), Optional.of(p.key()));
        DocumentLoader.Doc mine = DocumentLoader.myNotes(Optional.of(p.key()), problems.toString());

        assertEquals("statement", problemDoc.markdown());
        assertEquals(DocumentLoader.NO_SUPPLIED_NOTES, notesDoc.message());
        assertEquals(DocumentLoader.NO_SAVED_NOTES, mine.message());
        for (DocumentLoader.Doc d : new DocumentLoader.Doc[] {problemDoc, notesDoc, mine}) {
            String shown = String.valueOf(d.markdown()) + d.message();
            assertFalse(shown.contains("SECRET"));
        }
        assertTrue(Files.exists(solution) && Files.exists(cases));
    }

    @Test
    void sizeDescriptions() {
        assertEquals("1 KB", DocumentLoader.describeSize(10));
        assertEquals("1024 KB", DocumentLoader.describeSize(1024 * 1024 - 1));
        assertEquals("1.5 MB", DocumentLoader.describeSize(1536 * 1024));
    }
}
