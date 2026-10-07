package com.algoprep.upload;

import com.algoprep.bridge.ChatBridge;
import com.algoprep.problem.Problem;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Runs the upload sequence (Plan section 8.1): choose the files, stage them, and open ChatGPT's
 * file dialog. Pure: no Swing, and testable with a fake {@link ChatBridge}.
 *
 * <p>The sequence stops at the dialog. The user selects the files, adds a message if wanted and
 * presses Send in ChatGPT. Opening the dialog is not proof of an upload, so the messages never say
 * "attached", and nothing is retried automatically.
 */
public final class UploadService {

    public static final String NOTHING_SELECTED = "No problem selected.";
    public static final String UNAVAILABLE =
            "The selected problem's statement is no longer available. Press Refresh on the Problems tab.";
    public static final String BROWSER_NOT_READY =
            "ChatGPT is not ready. Wait for it to finish loading or log in, then try again.";

    private UploadService() {}

    /**
     * @param current        the selected problem while its statement is found, otherwise empty
     * @param key            the selected base key (present but with no {@code current} means the
     *                       statement is gone)
     * @param homeDir        the HOME setting, or null
     * @param problemsDir    the PROBLEMS setting, or null
     * @param stagingRoot    the effective staging root, or null if it could not be resolved
     * @param browserReady   whether the browser can take input now. Checked before the bridge is
     *                       reset, because a reset forces Ready from anywhere and would hide a
     *                       page that is still loading.
     * @param onStarted      called once the file dialog has been requested
     */
    public static void upload(Optional<Problem> current, Optional<String> key, String homeDir,
                              String problemsDir, Path stagingRoot, BooleanSupplier browserReady,
                              ChatBridge bridge, Consumer<String> status, Consumer<Problem> onStarted) {
        if (current.isEmpty()) {
            status.accept(key.isPresent() ? UNAVAILABLE : NOTHING_SELECTED);
            return;
        }
        Problem problem = current.get();

        UploadManifest manifest = UploadManifest.build(problem, homeDir);
        if (!manifest.ok()) {
            status.accept("Upload cancelled. " + manifest.problem());
            return;
        }

        Optional<String> invalidRoot = StagingFolder.validateRoot(stagingRoot, problemsDir, homeDir);
        if (invalidRoot.isPresent()) {
            status.accept("Upload cancelled. " + invalidRoot.get());
            return;
        }

        if (!browserReady.getAsBoolean()) {
            status.accept(BROWSER_NOT_READY);
            return;
        }

        // Reset-before-use mirrors ChatStory's DC23 workaround for stuck states (Plan section 6.1)
        bridge.reset();

        List<Path> staged;
        try {
            staged = StagingFolder.stage(stagingRoot, problemsDir, homeDir, manifest.files());
        } catch (StagingFolder.StagingException e) {
            status.accept("Upload cancelled. " + e.getMessage() + " Nothing was attached.");
            return;
        }

        bridge.clickUploadFile();
        status.accept(staged.size() + " file(s) staged. Select them in the ChatGPT file dialog.");
        onStarted.accept(problem);
    }
}
