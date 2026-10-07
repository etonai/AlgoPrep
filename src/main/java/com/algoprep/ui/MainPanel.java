package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.UiThread;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.config.SettingsStore;
import com.algoprep.instructions.InstructionsSender;
import com.algoprep.problem.SelectedProblemModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * MAIN tab (Plan section 6.1): the Problem, Instructions and Problem Files panels. The Problem panel
 * shows the selection (the "Last attached" part arrives in DC4), and Problem Files is a placeholder
 * until DC4.
 */
public class MainPanel extends JPanel {

    private static final int REFRESH_MS = 2000;

    private final AppState appState;
    private final SettingsStore settings;
    private final ChatBridge chatBridge;
    private final StatusReporter status;
    private final SelectedProblemModel selection;

    private final JLabel problemLabel = new JLabel(" ");
    private final JTextField pathField = new JTextField();
    private final JButton sendButton = new JButton("Send Instructions");
    private final JLabel sentIndicator = new JLabel(" ");

    public MainPanel(AppState appState, SettingsStore settings, ChatBridge chatBridge,
                     StatusReporter status, SelectedProblemModel selection) {
        super();
        this.appState = appState;
        this.settings = settings;
        this.chatBridge = chatBridge;
        this.status = status;
        this.selection = selection;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        add(problemPanel());
        add(Box.createVerticalStrut(6));
        add(instructionsPanel());
        add(Box.createVerticalStrut(6));
        add(problemFilesPanel());
        add(Box.createVerticalGlue());

        // The button depends on app state, the settings, and on the file itself, which can be
        // created or edited outside AlgoPrep, so the file is also re-checked on a slow timer.
        appState.addListener((prev, current) -> UiThread.run(() -> {
            if (current == AppState.State.LoadingChatGPT) {
                clearSentIndicator(); // the page reloaded or navigated, so "sent" is no longer known
            }
            refresh();
        }));
        settings.addListener(() -> UiThread.run(this::refresh));
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

    // ---- panels ----

    private JPanel problemPanel() {
        JPanel panel = titled("Problem");
        problemLabel.setText(selection.describe());
        selection.addListener(() -> UiThread.run(() -> problemLabel.setText(selection.describe())));
        panel.add(left(problemLabel));
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
        JButton upload = new JButton("Upload");
        upload.setEnabled(false);
        upload.setToolTipText("Upload is added in DC4");
        panel.add(left(upload));
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

    private void clearSentIndicator() {
        sentIndicator.setText(" ");
    }

    /** Updates the path field and the Send button's enabled state and tooltip. */
    private void refresh() {
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
        JPanel panel = new JPanel();
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
