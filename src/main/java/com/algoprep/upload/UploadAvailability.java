package com.algoprep.upload;

import com.algoprep.problem.Problem;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Decides whether Upload can be pressed and what the summary line says. One decision shared by
 * every Upload button, so they cannot drift apart. Pure: no Swing.
 */
public final class UploadAvailability {

    /**
     * @param summary the exact files Upload would attach, or the reason there is nothing to attach
     * @param blocker why Upload is disabled, or null if it can be pressed
     */
    public record Result(String summary, String blocker) {
        public boolean canUpload() {
            return blocker == null;
        }
    }

    public static final String COOLING_DOWN = "Upload was just started.";

    private UploadAvailability() {}

    /**
     * @param current        the selected problem while its statement is found, otherwise empty
     * @param key            the selected base key (present with no {@code current} means the
     *                       statement has gone)
     * @param stagingRoot    the effective staging root, or null if it could not be resolved
     * @param browserProblem why the browser cannot take input now, or null if it can
     * @param coolingDown    whether an upload was only just started
     */
    public static Result evaluate(Optional<Problem> current, Optional<String> key, String homeDir,
                                  String problemsDir, Path stagingRoot, String browserProblem,
                                  boolean coolingDown) {
        if (current.isEmpty()) {
            String reason = key.isPresent() ? UploadService.UNAVAILABLE : UploadService.NOTHING_SELECTED;
            return new Result(reason, reason);
        }

        UploadManifest manifest = UploadManifest.build(current.get(), homeDir);
        if (!manifest.ok()) {
            return new Result(manifest.problem(), manifest.problem());
        }
        String summary = String.join(", ", manifest.names());

        Optional<String> overlap = StagingFolder.validateRoot(stagingRoot, problemsDir, homeDir);
        if (overlap.isPresent()) {
            return new Result(summary, overlap.get() + " Fix it in Settings.");
        }
        if (browserProblem != null) {
            return new Result(summary, browserProblem);
        }
        if (coolingDown) {
            return new Result(summary, COOLING_DOWN);
        }
        return new Result(summary, null);
    }
}
