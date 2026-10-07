package com.algoprep.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Copies a problem's files into a folder that AlgoPrep owns, ready for ChatGPT's file dialog
 * (Plan section 8.2). Pure: no Swing.
 *
 * <p>ChatStory's {@code clearStaging()} deletes every regular file in a folder that can be set to
 * anything, so a staging folder set to HOME or PROBLEMS would delete the originals. This class
 * avoids that:
 * <ul>
 *   <li>the configured staging directory is a <em>root</em>, and files go into the fixed subfolder
 *       {@code <root>\algoprep-upload}, the only place anything is ever deleted;</li>
 *   <li>a root that equals, contains or is inside PROBLEMS or HOME is rejected;</li>
 *   <li>clearing is non-recursive and limited to regular files in that subfolder.</li>
 * </ul>
 * Paths are compared as absolute, normalized paths. Symlink containment is deliberately not
 * checked.
 */
public final class StagingFolder {

    public static final String SUBFOLDER = "algoprep-upload";

    /** An upload that must be aborted. The message is meant to be shown to the user. */
    public static final class StagingException extends Exception {
        public StagingException(String message) {
            super(message);
        }
    }

    private StagingFolder() {}

    public static Path subfolder(Path root) {
        return root.toAbsolutePath().normalize().resolve(SUBFOLDER);
    }

    /**
     * @return a message if the root cannot be used, or empty if it is fine. {@code problemsDir} and
     *         {@code homeDir} are the settings text and may be null or blank when unset.
     */
    public static Optional<String> validateRoot(Path root, String problemsDir, String homeDir) {
        if (root == null) {
            return Optional.of("The staging directory is not set.");
        }
        Path abs = root.toAbsolutePath().normalize();

        Optional<String> overlap = overlap(abs, problemsDir, "PROBLEMS");
        if (overlap.isPresent()) {
            return overlap;
        }
        overlap = overlap(abs, homeDir, "HOME");
        if (overlap.isPresent()) {
            return overlap;
        }
        if (Files.exists(abs) && !Files.isDirectory(abs)) {
            return Optional.of("The staging path is a file, not a directory: " + abs);
        }
        Path sub = abs.resolve(SUBFOLDER);
        if (Files.exists(sub) && !Files.isDirectory(sub)) {
            return Optional.of("The staging subfolder name is taken by a file: " + sub);
        }
        return Optional.empty();
    }

    /**
     * Validates, then replaces the contents of the staging subfolder with copies of
     * {@code sources}, under their original names. Either every file is staged or none is: any
     * failure removes what was copied in this attempt and throws.
     *
     * @return the staged files, in order
     */
    public static List<Path> stage(Path root, String problemsDir, String homeDir, List<Path> sources)
            throws StagingException {
        Optional<String> invalid = validateRoot(root, problemsDir, homeDir);
        if (invalid.isPresent()) {
            throw new StagingException(invalid.get());
        }

        // Check every source before touching the staging folder, so a bad one leaves it intact
        Set<String> names = new HashSet<>();
        for (Path source : sources) {
            if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
                throw new StagingException("Cannot read " + source.getFileName() + ".");
            }
            if (!names.add(source.getFileName().toString().toLowerCase())) {
                throw new StagingException("Two files would be staged as " + source.getFileName() + ".");
            }
        }

        Path sub = subfolder(root);
        try {
            Files.createDirectories(sub);
        } catch (IOException e) {
            throw new StagingException("Could not create the staging folder " + sub + ": " + e.getMessage());
        }
        clear(sub);

        List<Path> staged = new ArrayList<>();
        try {
            for (Path source : sources) {
                Path target = sub.resolve(source.getFileName().toString());
                Files.copy(source, target);
                staged.add(target);
            }
        } catch (IOException e) {
            for (Path copied : staged) {
                try {
                    Files.deleteIfExists(copied);
                } catch (IOException ignored) {
                    // best effort: the upload is aborted either way
                }
            }
            throw new StagingException("Could not copy files for upload: " + e.getMessage());
        }
        return List.copyOf(staged);
    }

    /** Deletes the regular files directly inside the subfolder. Nothing else is touched. */
    private static void clear(Path sub) throws StagingException {
        try (Stream<Path> stream = Files.list(sub)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                Files.delete(file);
            }
        } catch (IOException e) {
            throw new StagingException("Could not clear the previous staged files: " + e.getMessage());
        }
    }

    private static Optional<String> overlap(Path root, String other, String label) {
        if (other == null || other.isBlank()) {
            return Optional.empty();
        }
        Path dir;
        try {
            dir = Path.of(other).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        String reason;
        if (root.equals(dir)) {
            reason = "is the same folder as";
        } else if (root.startsWith(dir)) {
            reason = "is inside";
        } else if (dir.startsWith(root)) {
            reason = "contains";
        } else {
            return Optional.empty();
        }
        return Optional.of("The staging directory " + reason + " the " + label + " directory. "
                + "Choose a separate folder, so the originals can never be deleted.");
    }
}
