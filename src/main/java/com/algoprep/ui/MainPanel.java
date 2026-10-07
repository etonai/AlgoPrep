package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.UiThread;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.config.SettingsStore;
import com.algoprep.instructions.InstructionsSender;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.upload.UploadAvailability;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * MAIN tab (Plan section 6.1): the Problem, Instructions and Problem Files panels.
 *
 * <p>Problem shows the selection and, advisory only, the last problem whose files were staged for
 * upload. Instructions sends the instructions file and has a Reset for a send that gets stuck.
 * Problem Files has Upload and a summary of exactly which files it will attach. Upload itself is
 * shared with the Problems tab through {@link UploadController}.
 */
public class MainPanel extends JPanel {

    private static final int REFRESH_MS = 2000;

    /** Shown after Reset. It does not claim whether an earlier send arrived, because that is unknown. */
    static final String RESET_MESSAGE =
            "Reset. ChatGPT is set to Ready. If a message was being sent, check ChatGPT to see whether it arrived.";

    private final AppState appState;
    private final SettingsStore settings;
    private final ChatBridge chatBridge;
    private final StatusReporter status;
    private final SelectedProblemModel selection;
    private final UploadController uploads;
    private final StudiedController studiedControls;

    private final JLabel problemLabel = new JLabel(" ");
    private final JLabel lastAttachedLabel = new JLabel(" ");
    private final JTextField pathField = new JTextField();
    private final JButton sendButton = new JButton("Send Instructions");
    private final JButton resetButton = new JButton("Reset");
    private final JLabel sentIndicator = new JLabel(" ");
    private final JButton uploadButton = new JButton("Upload");
    private final JButton studiedButton = new JButton("Studied");
    private final JButton clearStudiedButton = new JButton("Clear Studied Tag");
    private final JTextArea uploadSummary = new JTextArea(3, 20);

    public MainPanel(AppState appState, SettingsStore settings, ChatBridge chatBridge,
                     StatusReporter status, SelectedProblemModel selection,
                     UploadController uploads, StudiedController studiedControls) {
        super();
        this.appState = appState;
        this.settings = settings;
        this.chatBridge = chatBridge;
        this.status = status;
        this.selection = selection;
        this.uploads = uploads;
        this.studiedControls = studiedControls;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(problemPanel());
        add(Box.createVerticalStrut(6));
        add(instructionsPanel());
        add(Box.createVerticalStrut(6));
        add(problemFilesPanel());
        add(Box.createVerticalGlue());

        // The Send button depends on app state, the settings and on the file itself, which can be
        // created or edited outside AlgoPrep, so the file is also re-checked on a slow timer.
        appState.addListener((prev, current) -> UiThread.run(() -> {
            if (current == AppState.State.LoadingChatGPT) {
                clearSentIndicator(); // the page reloaded or navigated, so "sent" is no longer known
            }
            refresh();
        }));
        settings.addListener(() -> UiThread.run(this::refresh));
        selection.addListener(() -> UiThread.run(this::refresh));
        Timer timer = new Timer(REFRESH_MS, e -> refresh());
        timer.setRepeats(true);
        timer.start();

        // Upload state lives in the shared controller. This panel only shows its summary.
        uploads.bind(uploadButton);
        uploads.addListener(this::showUpload);
        studiedControls.bindStudied(studiedButton);
        studiedControls.bindClear(clearStudiedButton);

        refresh();
    }

    /** Used by the Ctrl+Shift+I shortcut in DC5. */
    public void triggerSendInstructions() {
        if (sendButton.isEnabled()) {
            sendButton.doClick();
        }
    }

    /** Used by the Ctrl+Shift+U shortcut in DC5. Same trigger as the Problems tab's Upload. */
    public void triggerUpload() {
        uploads.trigger();
    }

    /** For a future shortcut. Same trigger as the Problems tab's Studied. */
    public void triggerStudied() {
        studiedControls.trigger();
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

        // Always enabled: it has to work exactly when everything else is disabled because a send
        // never got confirmed and the app state is stuck
        resetButton.setToolTipText("Force ChatGPT back to Ready if a send gets stuck. "
                + "Does not undo anything that was already sent.");
        resetButton.addActionListener(e -> resetState());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(browse);
        buttons.add(sendButton);
        buttons.add(resetButton);

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

        JLabel hint = new JLabel("In the file dialog, press Ctrl+A, then Open.");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(uploadButton);
        buttons.add(studiedButton);
        buttons.add(clearStudiedButton);

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

    /**
     * Same recovery as Ctrl+Shift+X: abandons any request still waiting and forces the app state
     * back to Ready, so Send Instructions and Upload can be used again.
     */
    private void resetState() {
        chatBridge.reset();
        // Reported after the reset, so it is not replaced by the "Ready" state label
        status.report(RESET_MESSAGE);
        refresh();
    }

    private void clearSentIndicator() {
        sentIndicator.setText(" ");
    }

    private void showUpload(UploadAvailability.Result result) {
        uploadSummary.setText(result.summary());
        uploadSummary.setCaretPosition(0);
        uploadSummary.setToolTipText("Staged in " + uploads.stagingSubfolder());
        lastAttachedLabel.setText(uploads.lastAttachedName().map(n -> "Last attached: " + n).orElse(" "));
    }

    /** Updates the labels and the Send button's enabled state and tooltip. */
    private void refresh() {
        problemLabel.setText(selection.describe());
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
