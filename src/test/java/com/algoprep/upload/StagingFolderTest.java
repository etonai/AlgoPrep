package com.algoprep.upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StagingFolderTest {

    @TempDir
    Path tmp;

    private Path problems;
    private Path home;
    private Path root;

    private void dirs() throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        home = Files.createDirectories(tmp.resolve("home"));
        root = tmp.resolve("staging");
    }

    private Path write(Path dir, String name, String content) throws IOException {
        Files.createDirectories(dir);
        return Files.writeString(dir.resolve(name), content);
    }

    private List<String> listNames(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private List<Path> stage(Path... sources) throws StagingFolder.StagingException {
        return StagingFolder.stage(root, problems.toString(), home.toString(), List.of(sources));
    }

    // ---- staging ----

    @Test
    void copiesFilesIntoTheOwnSubfolderUnderTheirOriginalNames() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        Path b = write(problems, "0001_a_notes.md", "B");

        List<Path> staged = stage(a, b);

        Path sub = root.resolve(StagingFolder.SUBFOLDER);
        assertEquals(List.of("0001_a_notes.md", "0001_a_problem.md"), listNames(sub));
        assertEquals(List.of(sub.resolve("0001_a_problem.md"), sub.resolve("0001_a_notes.md")), staged);
        assertEquals("A", Files.readString(sub.resolve("0001_a_problem.md")));
        assertEquals("A", Files.readString(a), "the original is untouched");
    }

    @Test
    void stagingAgainReplacesThePreviousProblemsFiles() throws Exception {
        dirs();
        Path first = write(problems, "0001_a_problem.md", "1");
        Path firstNotes = write(problems, "0001_a_notes.md", "1n");
        Path second = write(problems, "0002_b_problem.md", "2");

        stage(first, firstNotes);
        stage(second);

        assertEquals(List.of("0002_b_problem.md"), listNames(root.resolve(StagingFolder.SUBFOLDER)));
    }

    @Test
    void clearsOnlyItsOwnSubfolder() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        Path unrelatedInRoot = write(root, "keep-me.txt", "k");
        Path sub = root.resolve(StagingFolder.SUBFOLDER);
        write(sub, "old.md", "old");
        Path nestedFile = write(sub.resolve("nested"), "inside.txt", "n");

        stage(a);

        assertTrue(Files.exists(unrelatedInRoot), "files beside the subfolder are left alone");
        assertFalse(Files.exists(sub.resolve("old.md")), "old staged files are removed");
        assertTrue(Files.exists(nestedFile), "clearing is not recursive");
        assertTrue(Files.exists(sub.resolve("0001_a_problem.md")));
    }

    @Test
    void neverDeletesOriginalsInProblemsOrHome() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        Path mine = write(home, "0001_a_AlgoPrepNotes.md", "M");

        stage(a, mine);
        stage(a);

        assertTrue(Files.exists(a));
        assertTrue(Files.exists(mine));
    }

    // ---- root validation ----

    @Test
    void rejectsARootEqualToProblemsOrHome() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");

        for (Path bad : List.of(problems, home)) {
            var e = assertThrows(StagingFolder.StagingException.class,
                    () -> StagingFolder.stage(bad, problems.toString(), home.toString(), List.of(a)));
            assertTrue(e.getMessage().contains("same folder"), e.getMessage());
        }
        assertEquals(List.of("0001_a_problem.md"), listNames(problems), "nothing was created or deleted");
    }

    @Test
    void rejectsARootInsideProblemsOrHome() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        write(home, "keep.md", "k");

        for (Path bad : List.of(problems.resolve("staging"), home.resolve("deep").resolve("staging"))) {
            var e = assertThrows(StagingFolder.StagingException.class,
                    () -> StagingFolder.stage(bad, problems.toString(), home.toString(), List.of(a)));
            assertTrue(e.getMessage().contains("is inside"), e.getMessage());
        }
        assertEquals(List.of("0001_a_problem.md"), listNames(problems));
        assertEquals(List.of("keep.md"), listNames(home));
    }

    @Test
    void rejectsARootThatContainsProblemsOrHome() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");

        var e = assertThrows(StagingFolder.StagingException.class,
                () -> StagingFolder.stage(tmp, problems.toString(), home.toString(), List.of(a)));

        assertTrue(e.getMessage().contains("contains"), e.getMessage());
        assertFalse(Files.exists(tmp.resolve(StagingFolder.SUBFOLDER)));
    }

    @Test
    void comparesNormalizedPathsSoDotDotAndTrailingSeparatorsDoNotHideAnOverlap() throws Exception {
        dirs();
        Path sneaky = tmp.resolve("staging").resolve("..").resolve("problems");
        Path trailing = Path.of(problems + java.io.File.separator);

        assertTrue(StagingFolder.validateRoot(sneaky, problems.toString(), home.toString()).isPresent());
        assertTrue(StagingFolder.validateRoot(trailing, problems.toString(), home.toString()).isPresent());
        assertTrue(StagingFolder.validateRoot(root, problems + java.io.File.separator + ".", home.toString())
                .isEmpty());
    }

    @Test
    void comparesCaseInsensitivelyOnWindows() throws Exception {
        assumeTrue(System.getProperty("os.name").toLowerCase().contains("windows"));
        dirs();
        Path shouting = Path.of(problems.toString().toUpperCase());

        assertTrue(StagingFolder.validateRoot(shouting, problems.toString(), null).isPresent());
    }

    @Test
    void acceptsARootBesideProblemsAndHome() throws Exception {
        dirs();

        assertEquals(Optional.empty(), StagingFolder.validateRoot(root, problems.toString(), home.toString()));
    }

    @Test
    void siblingWithACommonPrefixIsNotAnOverlap() throws Exception {
        dirs();
        Path lookalike = tmp.resolve("problems-staging");

        assertEquals(Optional.empty(), StagingFolder.validateRoot(lookalike, problems.toString(), home.toString()));
    }

    @Test
    void unsetProblemsAndHomeAreIgnored() {
        assertEquals(Optional.empty(), StagingFolder.validateRoot(tmp.resolve("s"), null, "  "));
    }

    @Test
    void nullRootIsRejected() {
        assertTrue(StagingFolder.validateRoot(null, null, null).isPresent());
    }

    @Test
    void rootThatIsAFileIsRejected() throws Exception {
        dirs();
        Path file = write(tmp, "iamafile", "x");

        assertTrue(StagingFolder.validateRoot(file, problems.toString(), home.toString()).orElseThrow()
                .contains("is a file"));
    }

    @Test
    void subfolderNameTakenByAFileIsReported() throws Exception {
        dirs();
        write(root, StagingFolder.SUBFOLDER, "x");

        assertTrue(StagingFolder.validateRoot(root, problems.toString(), home.toString()).orElseThrow()
                .contains("taken by a file"));
    }

    // ---- failures ----

    @Test
    void aMissingSourceIsRejectedBeforeAnythingIsDeleted() throws Exception {
        dirs();
        Path good = write(problems, "0001_a_problem.md", "A");
        Path missing = problems.resolve("0001_a_notes.md");
        Path sub = root.resolve(StagingFolder.SUBFOLDER);
        write(sub, "previous.md", "p");

        var e = assertThrows(StagingFolder.StagingException.class, () -> stage(good, missing));

        assertTrue(e.getMessage().contains("0001_a_notes.md"), e.getMessage());
        assertEquals(List.of("previous.md"), listNames(sub), "the previous staging is intact");
    }

    @Test
    void aCopyFailureRemovesWhatWasCopiedAndAborts() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        Path b = write(problems, "0001_a_notes.md", "B");
        Path sub = root.resolve(StagingFolder.SUBFOLDER);
        // A directory in the way of the second target makes that copy fail after the first succeeded
        Files.createDirectories(sub.resolve("0001_a_notes.md"));

        assertThrows(StagingFolder.StagingException.class, () -> stage(a, b));

        assertFalse(Files.exists(sub.resolve("0001_a_problem.md")), "partial set must not be left behind");
    }

    @Test
    void twoSourcesWithTheSameNameAreRejected() throws Exception {
        dirs();
        Path a = write(problems, "same.md", "A");
        Path b = write(home, "same.md", "B");

        assertThrows(StagingFolder.StagingException.class, () -> stage(a, b));
    }

    @Test
    void aSubfolderThatIsAFileAbortsWithoutDeletingAnything() throws Exception {
        dirs();
        Path a = write(problems, "0001_a_problem.md", "A");
        Path blocker = write(root, StagingFolder.SUBFOLDER, "x");

        assertThrows(StagingFolder.StagingException.class, () -> stage(a));

        assertTrue(Files.exists(blocker));
        assertTrue(Files.exists(a));
    }
}
