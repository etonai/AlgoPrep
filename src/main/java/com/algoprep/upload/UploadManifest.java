package com.algoprep.upload;

import com.algoprep.notes.HomeNotes;
import com.algoprep.problem.Problem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The files an upload attaches for one problem (Plan section 8.1, step 1), in order: the statement,
 * the supplied notes if present, and the user's HOME notes if present. Nothing else is ever
 * included, so solutions and test cases cannot be uploaded. Pure: no Swing.
 *
 * <p>A file that does not exist is simply left out. A file that exists but cannot be read is a
 * {@code problem}, so the upload aborts instead of attaching a partial set silently.
 *
 * @param files   the files to attach, in order
 * @param problem why the upload cannot go ahead, or null if it can
 */
public record UploadManifest(List<Path> files, String problem) {

    public boolean ok() {
        return problem == null;
    }

    /** The file names, for the summary line. */
    public List<String> names() {
        return files.stream().map(p -> p.getFileName().toString()).toList();
    }

    /**
     * @param homeDir the HOME setting, or null if unset. A HOME that is not a directory is treated
     *                as having no notes (the Settings tab warns about it). A HOME that exists but
     *                cannot be listed is a problem.
     */
    public static UploadManifest build(Problem problem, String homeDir) {
        List<Path> files = new ArrayList<>();
        String error = null;

        String statementProblem = readableProblem(problem.statement());
        if (statementProblem != null) {
            error = statementProblem;
        } else {
            files.add(problem.statement());
        }

        Optional<Path> notes = problem.notes();
        if (notes.isPresent() && Files.exists(notes.get())) {
            String notesProblem = readableProblem(notes.get());
            if (notesProblem != null) {
                error = error != null ? error : notesProblem;
            } else {
                files.add(notes.get());
            }
        }

        if (homeDir != null && !homeDir.isBlank()) {
            Path home = toPath(homeDir);
            if (home != null && Files.isDirectory(home)) {
                try {
                    Optional<Path> mine = HomeNotes.find(home, problem.key());
                    if (mine.isPresent()) {
                        String mineProblem = readableProblem(mine.get());
                        if (mineProblem != null) {
                            error = error != null ? error : mineProblem;
                        } else {
                            files.add(mine.get());
                        }
                    }
                } catch (IOException e) {
                    error = error != null ? error : "Could not read the HOME directory: " + e.getMessage();
                }
            }
        }

        return new UploadManifest(List.copyOf(files), error);
    }

    private static String readableProblem(Path file) {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            return "Cannot read " + file.getFileName() + ".";
        }
        return null;
    }

    private static Path toPath(String text) {
        try {
            return Path.of(text);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
