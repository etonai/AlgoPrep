package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings tab (Plan section 6.3). This cycle has the theme and the PROBLEMS directory. The HOME
 * and staging folders are added in later cycles.
 */
public class SettingsPanel extends JPanel {

    private static final Color WARNING = new Color(0xC0, 0x50, 0x00);

    public SettingsPanel(NativeThemeModel themeModel, ProblemWorkspace workspace) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(sectionLabel("Theme"));
        content.add(themeControls(themeModel));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("PROBLEMS directory"));
        content.add(problemsControls(workspace));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(content, gbc);
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
     * Path label plus Browse. A saved path that no longer exists stays visible with a warning and
     * is not silently replaced.
     */
    private JPanel problemsControls(ProblemWorkspace workspace) {
        JLabel pathLabel = new JLabel();
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        JLabel warningLabel = new JLabel(" ");
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.PLAIN, 11f));
        warningLabel.setForeground(WARNING);

        Runnable refreshLabels = () -> {
            String text = workspace.directoryText();
            if (text == null || text.isBlank()) {
                pathLabel.setText("(not set)");
                pathLabel.setToolTipText(null);
                warningLabel.setText(" ");
                return;
            }
            pathLabel.setText(shorten(text));
            pathLabel.setToolTipText(text);
            warningLabel.setText(isDirectory(text) ? " " : "Warning: this directory was not found.");
        };
        workspace.catalog().addListener(() -> UiThread.run(refreshLabels));
        refreshLabels.run();

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select PROBLEMS Directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            String current = workspace.directoryText();
            if (current != null && !current.isBlank() && isDirectory(current)) {
                chooser.setCurrentDirectory(new File(current));
            }
            if (chooser.showOpenDialog(SettingsPanel.this) != JFileChooser.APPROVE_OPTION) return;
            workspace.setDirectory(chooser.getSelectedFile().toPath().toAbsolutePath());
        });

        return verticalPanel(pathLabel, warningLabel, browse);
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
