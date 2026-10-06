package com.chatstory.ui;

import com.chatstory.context.ContextFileStore;
import com.chatstory.controller.FinalControllerStore;
import com.chatstory.controller.IntermediateControllerStore;
import com.chatstory.controller.SessionControllerStore;
import com.chatstory.rules.RulesFileStore;
import com.chatstory.session.RedoCountStore;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class MainPanel extends JPanel {

    private final SessionControllerStore controllerStore;
    private final IntermediateControllerStore intermediateControllerStore;
    private final FinalControllerStore finalControllerStore;
    private final RulesFileStore rulesFileStore;
    private final ContextFileStore contextFileStore;
    private final RedoCountStore redoCountStore;
    private final Consumer<String> onSendPrompt;
    private final Runnable onClickUpload;
    private final Runnable onRedo;
    private final Runnable onContinue;
    private final Runnable onEndScene;
    private final Runnable onReset;
    private final Runnable onFetch;
    private final Runnable onEndSession;

    private JButton redoBtn;

    private final JTextField controllerPathField = new JTextField();
    private final JButton sendControllerButton = new JButton("Send Controller");
    private final JTextField intermediatePathField = new JTextField();
    private final JButton sendIntermediateButton = new JButton("Send Intermediate");
    private final JTextField finalPathField = new JTextField();
    private final JButton sendFinalButton = new JButton("Send Final");
    private final JButton sendRulesButton = new JButton("Send Rules");
    private final JLabel rulesStatusLabel = new JLabel(" ");
    private final JButton sendContextButton = new JButton("Send Context");
    private final JLabel contextStatusLabel = new JLabel(" ");

    public MainPanel(SessionControllerStore controllerStore,
                     IntermediateControllerStore intermediateControllerStore,
                     FinalControllerStore finalControllerStore,
                     RulesFileStore rulesFileStore,
                     ContextFileStore contextFileStore,
                     RedoCountStore redoCountStore,
                     Consumer<String> onSendPrompt,
                     Runnable onClickUpload,
                     Runnable onRedo,
                     Runnable onContinue,
                     Runnable onEndScene,
                     Runnable onReset,
                     Runnable onFetch,
                     Runnable onEndSession) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.controllerStore = controllerStore;
        this.intermediateControllerStore = intermediateControllerStore;
        this.finalControllerStore = finalControllerStore;
        this.rulesFileStore = rulesFileStore;
        this.contextFileStore = contextFileStore;
        this.redoCountStore = redoCountStore;
        this.onSendPrompt = onSendPrompt;
        this.onClickUpload = onClickUpload;
        this.onRedo = onRedo;
        this.onContinue = onContinue;
        this.onEndScene = onEndScene;
        this.onReset = onReset;
        this.onFetch = onFetch;
        this.onEndSession = onEndSession;

        add(buildControllerSection());
        add(Box.createVerticalStrut(8));
        add(buildRulesSection());
        add(Box.createVerticalStrut(8));
        add(buildContextSection());
        add(Box.createVerticalStrut(8));
        add(buildIntermediateControllerSection());
        add(Box.createVerticalStrut(8));
        add(buildFinalControllerSection());
        add(Box.createVerticalStrut(8));
        add(buildCommandsSection());
        add(Box.createVerticalStrut(8));
        add(buildEndSessionSection());
        add(Box.createVerticalGlue());

        refreshControllerPath();
        refreshIntermediatePath();
        refreshFinalPath();
        refreshSendRulesButton();
        refreshSendContextButton();

        controllerStore.addListener(this::refreshControllerPath);
        intermediateControllerStore.addListener(this::refreshIntermediatePath);
        finalControllerStore.addListener(this::refreshFinalPath);
        rulesFileStore.addListener(this::refreshSendRulesButton);
        contextFileStore.addCheckedListener(this::refreshSendContextButton);
        redoCountStore.addListener(this::updateRedoLabel);
    }

    private JPanel buildControllerSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Session Controller",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        controllerPathField.setEditable(false);
        controllerPathField.setFont(controllerPathField.getFont().deriveFont(11f));

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseController());

        sendControllerButton.addActionListener(e -> sendController());
        sendControllerButton.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(controllerPathField, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(sendControllerButton, gbc);

        return panel;
    }

    private JPanel buildIntermediateControllerSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Intermediate Controller",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        intermediatePathField.setEditable(false);
        intermediatePathField.setFont(intermediatePathField.getFont().deriveFont(11f));

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseIntermediate());

        sendIntermediateButton.addActionListener(e -> sendIntermediate());
        sendIntermediateButton.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(intermediatePathField, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(sendIntermediateButton, gbc);

        return panel;
    }

    private void refreshIntermediatePath() {
        SwingUtilities.invokeLater(() -> {
            Path path = intermediateControllerStore.getControllerFile();
            if (path != null) {
                intermediatePathField.setText(path.toString());
                sendIntermediateButton.setEnabled(true);
            } else {
                intermediatePathField.setText("");
                intermediatePathField.putClientProperty("JTextField.placeholderText", "No intermediate controller file selected");
                sendIntermediateButton.setEnabled(false);
            }
        });
    }

    private void browseIntermediate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Intermediate Controller File");

        Path current = intermediateControllerStore.getControllerFile();
        if (current != null) {
            chooser.setCurrentDirectory(current.getParent().toFile());
        }

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        intermediateControllerStore.setControllerFile(selected.toPath());
    }

    private void sendIntermediate() {
        Path path = intermediateControllerStore.getControllerFile();
        if (path == null) return;
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not read intermediate controller file:\n" + e.getMessage(),
                    "Send Intermediate Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        onSendPrompt.accept(content);
    }

    private JPanel buildFinalControllerSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Final Controller",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        finalPathField.setEditable(false);
        finalPathField.setFont(finalPathField.getFont().deriveFont(11f));

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseFinal());

        sendFinalButton.addActionListener(e -> sendFinal());
        sendFinalButton.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(finalPathField, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(sendFinalButton, gbc);

        return panel;
    }

    private void refreshFinalPath() {
        SwingUtilities.invokeLater(() -> {
            Path path = finalControllerStore.getControllerFile();
            if (path != null) {
                finalPathField.setText(path.toString());
                sendFinalButton.setEnabled(true);
            } else {
                finalPathField.setText("");
                finalPathField.putClientProperty("JTextField.placeholderText", "No final controller file selected");
                sendFinalButton.setEnabled(false);
            }
        });
    }

    private void browseFinal() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Final Controller File");

        Path current = finalControllerStore.getControllerFile();
        if (current != null) {
            chooser.setCurrentDirectory(current.getParent().toFile());
        }

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        finalControllerStore.setControllerFile(selected.toPath());
    }

    private void sendFinal() {
        Path path = finalControllerStore.getControllerFile();
        if (path == null) return;
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not read final controller file:\n" + e.getMessage(),
                    "Send Final Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        onSendPrompt.accept(content);
    }

    private JPanel buildRulesSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Rules",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        sendRulesButton.addActionListener(e -> sendRules());

        rulesStatusLabel.setFont(rulesStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        rulesStatusLabel.setForeground(Color.DARK_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(sendRulesButton, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 4, 4, 4);
        panel.add(rulesStatusLabel, gbc);

        return panel;
    }

    private void refreshControllerPath() {
        SwingUtilities.invokeLater(() -> {
            Path path = controllerStore.getControllerFile();
            if (path != null) {
                controllerPathField.setText(path.toString());
                sendControllerButton.setEnabled(true);
            } else {
                controllerPathField.setText("");
                controllerPathField.putClientProperty("JTextField.placeholderText", "No controller file selected");
                sendControllerButton.setEnabled(false);
            }
        });
    }

    private void refreshSendRulesButton() {
        SwingUtilities.invokeLater(() ->
                sendRulesButton.setEnabled(!rulesFileStore.getEntries().isEmpty()));
    }

    private void refreshSendContextButton() {
        SwingUtilities.invokeLater(() ->
                sendContextButton.setEnabled(!contextFileStore.getCheckedEntries().isEmpty()));
    }

    private void browseController() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Session Controller File");

        Path current = controllerStore.getControllerFile();
        if (current != null) {
            chooser.setCurrentDirectory(current.getParent().toFile());
        }

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        controllerStore.setControllerFile(selected.toPath());
    }

    private void sendController() {
        Path path = controllerStore.getControllerFile();
        if (path == null) return;
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not read controller file:\n" + e.getMessage(),
                    "Send Controller Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        redoCountStore.setCount(0);
        updateRedoLabel();
        onSendPrompt.accept(content);
    }

    private JPanel buildContextSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Context",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        sendContextButton.addActionListener(e -> sendContext());
        sendContextButton.setEnabled(false);

        contextStatusLabel.setFont(contextStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        contextStatusLabel.setForeground(Color.DARK_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(sendContextButton, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 4, 4, 4);
        panel.add(contextStatusLabel, gbc);

        return panel;
    }

    private void sendRules() {
        List<Path> files = rulesFileStore.getEntries();
        if (files.isEmpty()) return;

        onReset.run();
        contextFileStore.clearStaging();

        ContextFileStore.StagingResult result = contextFileStore.stageSelected(files);
        if (result.failed() > 0) {
            String failures = String.join("\n", result.failureMessages());
            JOptionPane.showMessageDialog(this,
                    "Failed to stage " + result.failed() + " file(s):\n" + failures
                            + "\n\nUpload aborted.",
                    "Send Rules Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        onClickUpload.run();
        rulesStatusLabel.setText(result.succeeded() + " file(s) staged — select them in the browser's upload dialog.");
    }

    private JPanel buildCommandsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Commands",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 60));

        redoBtn = new JButton("Redo " + redoCountStore.getCount());
        redoBtn.setToolTipText("Redo the last story beat");
        redoBtn.addActionListener(e -> {
            redoCountStore.setCount(redoCountStore.getCount() + 1);
            updateRedoLabel();
            onRedo.run();
        });

        JButton continueBtn = new JButton("Continue");
        continueBtn.setToolTipText("Continue the scene");
        continueBtn.addActionListener(e -> onContinue.run());

        JButton endSceneBtn = new JButton("End Scene");
        endSceneBtn.setToolTipText("End the current scene");
        endSceneBtn.addActionListener(e -> onEndScene.run());

        JButton resetBtn = new JButton("Reset");
        resetBtn.setToolTipText("Force app state back to Ready");
        resetBtn.addActionListener(e -> onReset.run());

        JButton fetchBtn = new JButton("Fetch");
        fetchBtn.setToolTipText("Force-read the current response from the browser");
        fetchBtn.addActionListener(e -> onFetch.run());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        row.setOpaque(false);
        row.add(redoBtn);
        row.add(continueBtn);
        row.add(endSceneBtn);
        row.add(resetBtn);
        row.add(fetchBtn);

        panel.add(row);

        return panel;
    }

    private JPanel buildEndSessionSection() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Session",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height + 40));

        JButton endSessionBtn = new JButton("End Session");
        endSessionBtn.setToolTipText("Append latest beat to canon (if needed) and save the canon file");
        endSessionBtn.setFont(endSessionBtn.getFont().deriveFont(Font.BOLD, 14f));
        endSessionBtn.addActionListener(e -> onEndSession.run());

        panel.add(endSessionBtn);
        return panel;
    }

    private void updateRedoLabel() {
        redoBtn.setText("Redo " + redoCountStore.getCount());
    }

    public void triggerSendContext() {
        sendContext();
    }

    private void sendContext() {
        List<Path> files = contextFileStore.getCheckedEntries();
        if (files.isEmpty()) return;

        onReset.run();
        contextFileStore.clearStaging();

        ContextFileStore.StagingResult result = contextFileStore.stageSelected(files);
        if (result.failed() > 0) {
            String failures = String.join("\n", result.failureMessages());
            JOptionPane.showMessageDialog(this,
                    "Failed to stage " + result.failed() + " file(s):\n" + failures
                            + "\n\nUpload aborted.",
                    "Send Context Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        onClickUpload.run();
        contextStatusLabel.setText(result.succeeded() + " file(s) staged — select them in the browser's upload dialog.");
    }
}
