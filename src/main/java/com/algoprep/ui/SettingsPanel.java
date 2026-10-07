package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.MarkdownConverter;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;
import com.algoprep.upload.StagingFolder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Settings tab (Plan section 6.3): the theme, and the PROBLEMS, HOME and staging directories.
 */
public class SettingsPanel extends JPanel {

    private static final Color WARNING = new Color(0xC0, 0x50, 0x00);

    /** How one directory setting differs from another. */
    private record DirectorySpec(
            String dialogTitle,
            Supplier<String> currentText,
            /** Extra text after the path, such as " (default)". May be null. */
            Supplier<String> labelSuffix,
            Consumer<Path> onChosen,
            /** Registers the label-refresh action with whatever signals a change. */
            Consumer<Runnable> subscribe,
            /** A warning for the saved path, or empty if it is fine. */
            Function<String, Optional<String>> warningFor,
            /** A reason to refuse a chosen directory, or empty to accept it. */
            Function<Path, Optional<String>> refuse,
            /** Returns to the default. Null if the setting has no default. */
            Runnable useDefault) { }

    public SettingsPanel(NativeThemeModel themeModel, ProblemWorkspace workspace,
                         SettingsStore settings, String defaultStagingRoot) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Function<String, Optional<String>> notFoundWarning = text ->
                isDirectory(text) ? Optional.empty() : Optional.of("Warning: this directory was not found.");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(sectionLabel("Theme"));
        content.add(themeControls(themeModel));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("PROBLEMS directory"));
        content.add(directoryControls(new DirectorySpec(
                "Select PROBLEMS Directory",
                workspace::directoryText,
                null,
                dir -> workspace.setDirectory(dir),
                // PROBLEMS has a catalog, so its labels follow the catalog's scans
                refresh -> workspace.catalog().addListener(() -> UiThread.run(refresh)),
                notFoundWarning,
                dir -> Optional.empty(),
                null)));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("HOME directory (your saved notes)"));
        content.add(directoryControls(new DirectorySpec(
                "Select HOME Directory",
                settings::getHomeDir,
                null,
                dir -> settings.setHomeDir(dir.toString()),
                // HOME is not scanned, so its labels follow the settings
                refresh -> settings.addListener(() -> UiThread.run(refresh)),
                notFoundWarning,
                dir -> Optional.empty(),
                null)));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("Upload staging directory"));
        content.add(directoryControls(new DirectorySpec(
                "Select Staging Directory",
                () -> settings.effectiveStagingRoot(defaultStagingRoot),
                () -> isDefaultStaging(settings) ? " (default)" : "",
                dir -> settings.setStagingRoot(dir.toString()),
                // The staging root must stay clear of PROBLEMS and HOME, so any of the three settings
                // changing can make it overlap
                refresh -> settings.addListener(() -> UiThread.run(refresh)),
                // The directory itself need not exist: it is created at the first upload
                text -> stagingProblem(settings, text),
                dir -> StagingFolder.validateRoot(dir, settings.getProblemsDir(), settings.getHomeDir()),
                () -> settings.setStagingRoot(null))));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(content, gbc);
    }

    private static boolean isDefaultStaging(SettingsStore settings) {
        String saved = settings.getStagingRoot();
        return saved == null || saved.isBlank();
    }

    private static Optional<String> stagingProblem(SettingsStore settings, String text) {
        try {
            return StagingFolder.validateRoot(Path.of(text), settings.getProblemsDir(), settings.getHomeDir())
                    .map(message -> "Warning: " + message + " Uploads are refused until this is fixed.");
        } catch (RuntimeException e) {
            return Optional.of("Warning: this is not a valid path.");
        }
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel themeControls(NativeThemeModel model) {
        JRadioButton dark = new JRadioButton("Dark Mode");
        JRadioButton light = new JRadioButton("Light Mode");
        ButtonGroup group = new ButtonGroup();
        group.add(dark);
        group.add(light);
        dark.setSelected(model.current() == NativeTheme.DARK);
        light.setSelected(model.current() == NativeTheme.LIGHT);

        dark.addActionListener(e -> model.setTheme(NativeTheme.DARK));
        light.addActionListener(e -> model.setTheme(NativeTheme.LIGHT));
        model.addListener((previous, current) -> UiThread.run(() -> {
            dark.setSelected(current == NativeTheme.DARK);
            light.setSelected(current == NativeTheme.LIGHT);
        }));

        return verticalPanel(dark, light);
    }

    /**
     * A path label, a warning line and Browse, shared by the directory settings. A saved path that
     * is not usable stays visible with a warning and is not silently replaced.
     */
    private JPanel directoryControls(DirectorySpec spec) {
        JLabel pathLabel = new JLabel();
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel warningLabel = new JLabel(" ");
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.PLAIN, 11f));
        warningLabel.setForeground(WARNING);

        Runnable refreshLabels = () -> {
            String text = spec.currentText().get();
            if (text == null || text.isBlank()) {
                pathLabel.setText("(not set)");
                pathLabel.setToolTipText(null);
                warningLabel.setText(" ");
                warningLabel.setToolTipText(null);
                return;
            }
            String suffix = spec.labelSuffix() == null ? "" : spec.labelSuffix().get();
            pathLabel.setText(shorten(text) + suffix);
            pathLabel.setToolTipText(text);
            Optional<String> warning = spec.warningFor().apply(text);
            // HTML so a long warning wraps inside the narrow right pane
            warningLabel.setText(warning
                    .map(w -> "<html><body style='width: 250px'>" + MarkdownConverter.escape(w) + "</body></html>")
                    .orElse(" "));
            warningLabel.setToolTipText(warning.orElse(null));
        };
        spec.subscribe().accept(refreshLabels);
        refreshLabels.run();

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(spec.dialogTitle());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            String current = spec.currentText().get();
            if (current != null && !current.isBlank() && isDirectory(current)) {
                chooser.setCurrentDirectory(new File(current));
            }
            if (chooser.showOpenDialog(SettingsPanel.this) != JFileChooser.APPROVE_OPTION) return;
            Path chosen = chooser.getSelectedFile().toPath().toAbsolutePath();
            Optional<String> refusal = spec.refuse().apply(chosen);
            if (refusal.isPresent()) {
                JOptionPane.showMessageDialog(SettingsPanel.this, refusal.get(),
                        "Folder Not Allowed", JOptionPane.ERROR_MESSAGE);
                return; // the setting is left as it was
            }
            spec.onChosen().accept(chosen);
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(browse);
        if (spec.useDefault() != null) {
            JButton useDefault = new JButton("Use Default");
            useDefault.setToolTipText("Go back to the default folder");
            useDefault.addActionListener(e -> spec.useDefault().run());
            buttons.add(useDefault);
        }

        return verticalPanel(pathLabel, warningLabel, buttons);
    }

    private static boolean isDirectory(String text) {
        try {
            return Files.isDirectory(Path.of(text));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String shorten(String path) {
        if (path.length() <= 50) return path;
        return "…" + path.substring(path.length() - 47);
    }

    private JPanel verticalPanel(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JComponent component : components) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(component);
        }
        return panel;
    }
}
