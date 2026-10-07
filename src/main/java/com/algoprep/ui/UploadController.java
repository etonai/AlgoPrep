package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.UiThread;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.upload.StagingFolder;
import com.algoprep.upload.UploadAvailability;
import com.algoprep.upload.UploadService;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The one Upload implementation, shared by every Upload button (the MAIN tab and the Problems
 * tab). It owns what must be the same everywhere: whether Upload is allowed and why not, the
 * cooldown, the advisory "last attached" problem, and the call to {@link UploadService}.
 *
 * <p>Buttons are attached with {@link #bind}. Pressing any of them runs the same upload and
 * updates the same state, so they cannot drift apart. Everything is re-checked when the app state,
 * the settings or the selection change, and on a slow timer, because files can appear or disappear
 * outside AlgoPrep. Must be used on the Swing thread.
 */
public class UploadController {

    private static final int REFRESH_MS = 2000;
    /** Stops a double-click from opening two file dialogs. */
    private static final int COOLDOWN_MS = 2000;

    private final AppState appState;
    private final SettingsStore settings;
    private final ChatBridge chatBridge;
    private final StatusReporter status;
    private final SelectedProblemModel selection;
    private final String defaultStagingRoot;
    private final int cooldownMs;

    private final List<JButton> buttons = new ArrayList<>();
    private final List<Consumer<UploadAvailability.Result>> listeners = new ArrayList<>();

    private UploadAvailability.Result latest = new UploadAvailability.Result("", UploadAvailability.COOLING_DOWN);
    private String lastAttachedName;
    private boolean coolingDown;

    public UploadController(AppState appState, SettingsStore settings, ChatBridge chatBridge,
                            StatusReporter status, SelectedProblemModel selection,
                            String defaultStagingRoot) {
        this(appState, settings, chatBridge, status, selection, defaultStagingRoot, COOLDOWN_MS);
    }

    /** Test seam: the cooldown length can be shortened. */
    UploadController(AppState appState, SettingsStore settings, ChatBridge chatBridge,
                     StatusReporter status, SelectedProblemModel selection,
                     String defaultStagingRoot, int cooldownMs) {
        this.appState = appState;
        this.settings = settings;
        this.chatBridge = chatBridge;
        this.status = status;
        this.selection = selection;
        this.defaultStagingRoot = defaultStagingRoot;
        this.cooldownMs = cooldownMs;

        appState.addListener((prev, current) -> UiThread.run(() -> {
            if (current == AppState.State.LoadingChatGPT) {
                // The page reloaded or navigated, so "attached" is no longer known
                clearLastAttached();
            }
            refresh();
        }));
        settings.addListener(() -> UiThread.run(this::refresh));
        selection.addListener(() -> UiThread.run(this::refresh));
        Timer timer = new Timer(REFRESH_MS, e -> refresh());
        timer.setRepeats(true);
        timer.start();

        refresh();
    }

    /** Attaches an Upload button: pressing it uploads, and it follows the shared enabled state. */
    public void bind(JButton button) {
        button.addActionListener(e -> upload());
        buttons.add(button);
        apply(button);
    }

    /** Called with the current result now, and again whenever it is re-evaluated. */
    public void addListener(Consumer<UploadAvailability.Result> listener) {
        listeners.add(listener);
        listener.accept(latest);
    }

    /** The advisory "last attached" problem, shown on the MAIN tab. */
    public Optional<String> lastAttachedName() {
        return Optional.ofNullable(lastAttachedName);
    }

    /** Where the files are staged, for tooltips. */
    public Path stagingSubfolder() {
        Path root = stagingRoot();
        return StagingFolder.subfolder(root != null ? root : Path.of(defaultStagingRoot));
    }

    /** One trigger for every button, used by the Ctrl+Shift+U shortcut in DC5. */
    public void trigger() {
        if (latest.canUpload()) {
            upload();
        }
    }

    // ---- behavior ----

    private void upload() {
        // No automatic retry: a second press is the user's decision, and a short cooldown stops
        // an accidental double-click from opening two dialogs
        coolingDown = true;
        refresh();
        Timer cooldown = new Timer(cooldownMs, e -> {
            coolingDown = false;
            refresh();
        });
        cooldown.setRepeats(false);
        cooldown.start();

        UploadService.upload(selection.current(), selection.selectedKey(), settings.getHomeDir(),
                settings.getProblemsDir(), stagingRoot(), appState::isSendEnabled, chatBridge,
                status::report, problem -> UiThread.run(() -> {
                    lastAttachedName = problem.displayName();
                    refresh();
                }));
    }

    private void clearLastAttached() {
        lastAttachedName = null;
    }

    /** The effective staging root, or null if the saved setting is not a valid path. */
    private Path stagingRoot() {
        try {
            return Path.of(settings.effectiveStagingRoot(defaultStagingRoot));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void refresh() {
        latest = UploadAvailability.evaluate(selection.current(), selection.selectedKey(),
                settings.getHomeDir(), settings.getProblemsDir(), stagingRoot(),
                appState.isSendEnabled() ? null : "ChatGPT is not ready (" + appState.current() + ")",
                coolingDown);
        buttons.forEach(this::apply);
        new ArrayList<>(listeners).forEach(l -> l.accept(latest));
    }

    private void apply(JButton button) {
        button.setEnabled(latest.canUpload());
        button.setToolTipText(latest.canUpload()
                ? "Stage these files and open ChatGPT's file dialog" : latest.blocker());
    }
}
