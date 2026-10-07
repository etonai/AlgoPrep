package com.algoprep.upload;

import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.problem.Problem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UploadServiceTest {

    @TempDir
    Path tmp;

    private Path problems;
    private Path home;
    private Path root;

    private final List<String> calls = new ArrayList<>();
    private final List<String> status = new ArrayList<>();
    private final List<Problem> started = new ArrayList<>();

    private final ChatBridge bridge = new ChatBridge() {
        @Override public void sendPrompt(String prompt, ResponseListener l) { calls.add("sendPrompt"); }
        @Override public void sendRawPrompt(String prompt, ResponseListener l) { calls.add("sendRawPrompt"); }
        @Override public void reset() { calls.add("reset"); }
        @Override public void clickUploadFile() { calls.add("click"); }
    };

    private void dirs() throws IOException {
        problems = Files.createDirectories(tmp.resolve("problems"));
        home = Files.createDirectories(tmp.resolve("home"));
        root = tmp.resolve("staging");
    }

    private Path write(Path dir, String name) throws IOException {
        return Files.writeString(dir.resolve(name), "x");
    }

    private Problem problem(Path statement, Path notes) {
        return new Problem("0001_two-sum", 1, "Two Sum", statement, Optional.ofNullable(notes));
    }

    private void upload(Optional<Problem> current, Optional<String> key, String homeDir,
                        Path stagingRoot, boolean ready) {
        UploadService.upload(current, key, homeDir, problems == null ? null : problems.toString(),
                stagingRoot, () -> ready, bridge, status::add, started::add);
    }

    private List<String> stagedNames() throws IOException {
        Path sub = root.resolve(StagingFolder.SUBFOLDER);
        if (!Files.isDirectory(sub)) return List.of();
        try (Stream<Path> s = Files.list(sub)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void successResetsStagesThenOpensTheDialogAndReportsHonestly() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Path notes = write(problems, "0001_two-sum_notes.md");
        write(home, "0001_two-sum_AlgoPrepNotes.md");
        Problem p = problem(statement, notes);

        upload(Optional.of(p), Optional.of(p.key()), home.toString(), root, true);

        assertEquals(List.of("reset", "click"), calls);
        assertEquals(List.of("0001_two-sum_AlgoPrepNotes.md", "0001_two-sum_notes.md",
                "0001_two-sum_problem.md"), stagedNames());
        assertEquals(List.of("3 file(s) staged. Select them in the ChatGPT file dialog."), status);
        assertEquals(List.of(p), started);
        assertFalse(status.get(0).toLowerCase().contains("attached"));
    }

    @Test
    void statementOnlyProblemStagesOneFile() throws IOException {
        dirs();
        Problem p = problem(write(problems, "0001_two-sum_problem.md"), null);

        upload(Optional.of(p), Optional.of(p.key()), null, root, true);

        assertEquals(List.of("1 file(s) staged. Select them in the ChatGPT file dialog."), status);
    }

    @Test
    void nothingSelectedDoesNothing() throws IOException {
        dirs();

        upload(Optional.empty(), Optional.empty(), null, root, true);

        assertTrue(calls.isEmpty());
        assertEquals(List.of(UploadService.NOTHING_SELECTED), status);
        assertTrue(started.isEmpty());
    }

    @Test
    void unavailableProblemDoesNothing() throws IOException {
        dirs();

        upload(Optional.empty(), Optional.of("0001_two-sum"), null, root, true);

        assertTrue(calls.isEmpty());
        assertEquals(List.of(UploadService.UNAVAILABLE), status);
    }

    @Test
    void browserNotReadyDoesNotResetStageOrClick() throws IOException {
        dirs();
        Problem p = problem(write(problems, "0001_two-sum_problem.md"), null);

        upload(Optional.of(p), Optional.of(p.key()), null, root, false);

        assertTrue(calls.isEmpty(), "reset would force Ready and hide a loading page");
        assertEquals(List.of(UploadService.BROWSER_NOT_READY), status);
        assertEquals(List.of(), stagedNames());
        assertTrue(started.isEmpty());
    }

    @Test
    void unreadableFileCancelsBeforeAnythingElse() throws IOException {
        dirs();
        Problem p = problem(problems.resolve("0001_two-sum_problem.md"), null); // never created

        upload(Optional.of(p), Optional.of(p.key()), null, root, true);

        assertTrue(calls.isEmpty());
        assertEquals(1, status.size());
        assertTrue(status.get(0).startsWith("Upload cancelled."), status.get(0));
        assertTrue(started.isEmpty());
    }

    @Test
    void overlappingStagingRootIsRefusedAndNothingIsDeleted() throws IOException {
        dirs();
        Path statement = write(problems, "0001_two-sum_problem.md");
        Problem p = problem(statement, null);

        for (Path bad : List.of(problems, home, problems.resolve("sub"), tmp)) {
            calls.clear();
            status.clear();
            upload(Optional.of(p), Optional.of(p.key()), home.toString(), bad, true);

            assertTrue(calls.isEmpty(), bad.toString());
            assertTrue(status.get(0).startsWith("Upload cancelled."), status.get(0));
        }
        assertTrue(Files.exists(statement));
        assertTrue(started.isEmpty());
    }

    @Test
    void unresolvedStagingRootIsRefused() throws IOException {
        dirs();
        Problem p = problem(write(problems, "0001_two-sum_problem.md"), null);

        upload(Optional.of(p), Optional.of(p.key()), null, null, true);

        assertTrue(calls.isEmpty());
        assertTrue(status.get(0).startsWith("Upload cancelled."));
    }

    @Test
    void stagingFailureResetsButNeverOpensTheDialog() throws IOException {
        dirs();
        Problem p = problem(write(problems, "0001_two-sum_problem.md"), null);
        // A directory where the staged file must go makes the copy fail
        Files.createDirectories(root.resolve(StagingFolder.SUBFOLDER).resolve("0001_two-sum_problem.md"));

        upload(Optional.of(p), Optional.of(p.key()), null, root, true);

        assertEquals(List.of("reset"), calls, "click must not be called after a failed staging");
        assertTrue(status.get(0).startsWith("Upload cancelled."), status.get(0));
        assertTrue(status.get(0).contains("Nothing was attached"), status.get(0));
        assertTrue(started.isEmpty());
    }

    @Test
    void uploadingAnotherProblemLeavesOnlyItsFilesStaged() throws IOException {
        dirs();
        Problem one = problem(write(problems, "0001_two-sum_problem.md"), write(problems, "0001_two-sum_notes.md"));
        Problem two = new Problem("0002_add", 2, "Add", write(problems, "0002_add_problem.md"), Optional.empty());

        upload(Optional.of(one), Optional.of(one.key()), null, root, true);
        upload(Optional.of(two), Optional.of(two.key()), null, root, true);

        assertEquals(List.of("0002_add_problem.md"), stagedNames());
    }
}
