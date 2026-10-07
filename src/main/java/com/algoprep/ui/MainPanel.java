package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.UiThread;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.config.SettingsStore;
import com.algoprep.instructions.InstructionsSender;
import com.algoprep.problem.Problem;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.upload.StagingFolder;
import com.algoprep.upload.UploadManifest;
import com.algoprep.upload.UploadService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * MAIN tab (Plan section 6.1): the Problem, Instructions and Problem Files panels.
 *
 * <p>Problem shows the selection and, advisory only, the last problem whose files were staged for
 * upload. Problem Files has Upload and a summary of exactly which files it will attach.
 */
public class MainPanel extends JPanel {

    private static final int REFRESH_MS = 2000;
    /** Stops a double-click from opening two file dialogs. */
    private static final int UPLOAD_COOLDOWN_MS = 2000;

    private final AppState appState;
    private final SettingsStore settings;
    private final ChatBridge chatBridge;
    private final StatusReporter status;
    private final SelectedProblemModel selection;
    private final String defaultStagingRoot;

    private final JLabel problemLabel = new JLabel(" ");
    private final JLabel lastAttachedLabel = new JLabel(" ");
    private final JTextField pathField = new JTextField();
    private final JButton sendButton = new JButton("Send Instructions");
    private final JLabel sentIndicator = new JLabel(" ");
    private final JButton uploadButton = new JButton("Upload");
    private final JTextArea uploadSummary = new JTextArea(3, 20);

    private String lastAttachedName;
    private boolean uploadCoolingDown;

    public MainPanel(AppState appState, SettingsStore settings, ChatBridge chatBridge,
                     StatusReporter status, SelectedProblemModel selection,
                     String defaultStagingRoot) {
        super();
        this.appState = appState;
        this.settings = settings;
        this.chatBridge = chatBridge;
        this.status = status;
        this.selection = selection;
        this.defaultStagingRoot = defaultStagingRoot;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(problemPanel());
        add(Box.createVerticalStrut(6));
        add(instructionsPanel());
        add(Box.createVerticalStrut(6));
        add(problemFilesPanel());
        add(Box.createVerticalGlue());

        // The buttons depend on app state, the settings and the files themselves, which can be
        // created or edited outside AlgoPrep, so everything is also re-checked on a slow timer.
        appState.addListener((prev, current) -> UiThread.run(() -> {
            if (current == AppState.State.LoadingChatGPT) {
                // The page reloaded or navigated, so neither "sent" nor "attached" is still known
                clearSentIndicator();
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

    /** Used by the Ctrl+Shift+I shortcut in DC5. */
    public void triggerSendInstructions() {
        if (sendButton.isEnabled()) {
            sendButton.doClick();
        }
    }

    /** Used by the Ctrl+Shift+U shortcut in DC5. */
    public void triggerUpload() {
        if (uploadButton.isEnabled()) {
            uploadButton.doClick();
        }
    }

    // ---- panels ----

    private JPanel problemPanel() {
        JPanel panel = titled("Problem");
        lastAttachedLabel.setToolTipText("Advisory: this problem's files were staged and the file "
                + "dialog was opened. It is not confirmation that ChatGPT received them.");
        panel.add(left(problemLabel));
        panel.add(left(lastAttachedLabel));
        return panel;
    }

    private JPanel instructionsPanel() {
        JPanel panel = titled("Instructions");

        pathField.setEditable(false);
        pathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathField.getPreferredSize().height));

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> browseForInstructions());
        sendButton.addActionListener(e -> sendInstructions());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(browse);
        buttons.add(sendButton);

        panel.add(left(pathField));
        panel.add(Box.createVerticalStrut(4));
        panel.add(left(buttons));
        panel.add(Box.createVerticalStrut(4));
        panel.add(left(sentIndicator));
        return panel;
    }

    private JPanel problemFilesPanel() {
        JPanel panel = titled("Problem Files");

        uploadSummary.setEditable(false);
        uploadSummary.setLineWrap(true);
        uploadSummary.setWrapStyleWord(true);
        uploadSummary.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        uploadSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, uploadSummary.getPreferredSize().height));

        uploadButton.addActionListener(e -> upload());

        JLabel hint = new JLabel("In the file dialog, press Ctrl+A, then Open.");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(uploadButton);

        panel.add(left(uploadSummary));
        panel.add(Box.createVerticalStrut(4));
        panel.add(left(buttons));
        panel.add(Box.createVerticalStrut(2));
        panel.add(left(hint));
        return panel;
    }

    // ---- behavior ----

    private void browseForInstructions() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Instructions File");
        String current = settings.getInstructionsFile();
        if (current != null && !current.isBlank()) {
            File parent = new File(current).getParentFile();
            if (parent != null && parent.isDirectory()) {
                chooser.setCurrentDirectory(parent);
            }
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        settings.setInstructionsFile(chooser.getSelectedFile().toPath().toAbsolutePath().toString());
    }

    private void sendInstructions() {
        String path = settings.getInstructionsFile();
        InstructionsSender.send(path, appState::isSendEnabled, chatBridge, status::report, () ->
                UiThread.run(() -> {
                    String name = Path.of(path).getFileName().toString();
                    sentIndicator.setText(" Instructions sent: " + name + " at "
                            + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            + " (advisory)");
                }));
    }

    private void upload() {
        // No automatic retry: a second press is the user's decision, and a short cooldown stops
        // an accidental double-click from opening two dialogs
        uploadCoolingDown = true;
        refresh();
        Timer cooldown = new Timer(UPLOAD_COOLDOWN_MS, e -> {
            uploadCoolingDown = false;
            refresh();
        });
        cooldown.setRepeats(false);
        cooldown.start();

        UploadService.upload(selection.current(), selection.selectedKey(), settings.getHomeDir(),
                settings.getProblemsDir(), stagingRoot(), appState::isSendEnabled, chatBridge,
                status::report, problem -> UiThread.run(() -> {
                    lastAttachedName = problem.displayName();
                    lastAttachedLabel.setText("Last attached: " + lastAttachedName);
                }));
    }

    private void clearSentIndicator() {
        sentIndicator.setText(" ");
    }

    private void clearLastAttached() {
        lastAttachedName = null;
        lastAttachedLabel.setText(" ");
    }

    /** The effective staging root, or null if the saved setting is not a valid path. */
    private Path stagingRoot() {
        try {
            return Path.of(settings.effectiveStagingRoot(defaultStagingRoot));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Updates the labels, the summary and the buttons' enabled state and tooltips. */
    private void refresh() {
        problemLabel.setText(selection.describe());
        lastAttachedLabel.setText(lastAttachedName == null ? " " : "Last attached: " + lastAttachedName);
        refreshInstructions();
        refreshUpload();
    }

    private void refreshInstructions() {
        String path = settings.getInstructionsFile();
        pathField.setText(path == null ? "" : path);
        pathField.setToolTipText(path);

        String reason = null;
        InstructionsSender.Loaded loaded = InstructionsSender.load(path);
        if (!loaded.ok()) {
            reason = loaded.problem();
        } else if (!appState.isSendEnabled()) {
            reason = "ChatGPT is not ready (" + appState.current() + ")";
        }
        sendButton.setEnabled(reason == null);
        sendButton.setToolTipText(reason == null ? "Send the instructions file to ChatGPT" : reason);
    }

    private void refreshUpload() {
        Optional<Problem> current = selection.current();
        String summary;
        String reason = null;

        if (current.isEmpty()) {
            reason = selection.selectedKey().isPresent() ? UploadService.UNAVAILABLE : UploadService.NOTHING_SELECTED;
            summary = reason;
        } else {
            UploadManifest manifest = UploadManifest.build(current.get(), settings.getHomeDir());
            if (manifest.ok()) {
                summary = String.join(", ", manifest.names());
            } else {
                reason = manifest.problem();
                summary = reason;
            }
            if (reason == null) {
                Optional<String> overlap = StagingFolder.validateRoot(
                        stagingRoot(), settings.getProblemsDir(), settings.getHomeDir());
                if (overlap.isPresent()) {
                    reason = overlap.get() + " Fix it in Settings.";
                }
            }
            if (reason == null && !appState.isSendEnabled()) {
                reason = "ChatGPT is not ready (" + appState.current() + ")";
            }
            if (reason == null && uploadCoolingDown) {
                reason = "Upload was just started.";
            }
        }

        uploadSummary.setText(summary);
        uploadSummary.setCaretPosition(0);
        uploadSummary.setToolTipText("Staged in " + StagingFolder.subfolder(
                stagingRootOrDefault()));
        uploadButton.setEnabled(reason == null);
        uploadButton.setToolTipText(reason == null
                ? "Stage these files and open ChatGPT's file dialog" : reason);
    }

    private Path stagingRootOrDefault() {
        Path root = stagingRoot();
        return root != null ? root : Path.of(defaultStagingRoot);
    }

    // ---- layout helpers ----

    private static JPanel titled(String title) {
        // Capped at its preferred height so spare room goes below the panels, not inside them
        JPanel panel = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
