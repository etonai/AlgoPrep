package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.context.ContextFileStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.theme.NativeTheme;
import com.chatstory.theme.NativeThemeModel;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class ConfigurationPanel extends JPanel {

    public ConfigurationPanel(AppModeModel modeModel, NativeThemeModel themeModel,
                              ContextFileStore contextFileStore, CanonFolderStore canonFolderStore) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(sectionLabel("Mode"));
        content.add(modeControls(modeModel));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("Theme"));
        content.add(themeControls(themeModel));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("Staging Folder"));
        content.add(stagingFolderControls(contextFileStore));
        content.add(Box.createVerticalStrut(16));
        content.add(sectionLabel("Canon Folder"));
        content.add(canonFolderControls(canonFolderStore));

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

    private JPanel modeControls(AppModeModel model) {
        JRadioButton story = new JRadioButton("Story Mode");
        JRadioButton unassisted = new JRadioButton("Unassisted Mode");
        ButtonGroup group = new ButtonGroup();
        group.add(story);
        group.add(unassisted);
        story.setSelected(model.current() == AppMode.STORY);
        unassisted.setSelected(model.current() == AppMode.UNASSISTED);

        story.addActionListener(e -> model.setMode(AppMode.STORY));
        unassisted.addActionListener(e -> model.setMode(AppMode.UNASSISTED));
        model.addListener((previous, current) -> UiThread.run(() -> {
            story.setSelected(current == AppMode.STORY);
            unassisted.setSelected(current == AppMode.UNASSISTED);
        }));

        return verticalPanel(story, unassisted);
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

    private JPanel stagingFolderControls(ContextFileStore contextFileStore) {
        JLabel pathLabel = new JLabel(shorten(contextFileStore.getStagingPath().toString()));
        pathLabel.setToolTipText(contextFileStore.getStagingPath().toString());
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Staging Folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(contextFileStore.getStagingPath().toFile());

            if (chooser.showOpenDialog(ConfigurationPanel.this) != JFileChooser.APPROVE_OPTION) return;

            Path selected = chooser.getSelectedFile().toPath().toAbsolutePath();
            contextFileStore.setStagingPath(selected);
            pathLabel.setText(shorten(selected.toString()));
            pathLabel.setToolTipText(selected.toString());
        });

        return verticalPanel(pathLabel, browseButton);
    }

    private JPanel canonFolderControls(CanonFolderStore canonFolderStore) {
        String initial = canonFolderStore.getCanonFolder() != null
                ? shorten(canonFolderStore.getCanonFolder().toString())
                : "(not set)";
        JLabel pathLabel = new JLabel(initial);
        if (canonFolderStore.getCanonFolder() != null) {
            pathLabel.setToolTipText(canonFolderStore.getCanonFolder().toString());
        }
        pathLabel.setFont(pathLabel.getFont().deriveFont(Font.PLAIN, 11f));
        pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        canonFolderStore.addListener(() -> UiThread.run(() -> {
            java.nio.file.Path p = canonFolderStore.getCanonFolder();
            pathLabel.setText(p != null ? shorten(p.toString()) : "(not set)");
            pathLabel.setToolTipText(p != null ? p.toString() : null);
        }));

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Canon Folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (canonFolderStore.getCanonFolder() != null) {
                chooser.setCurrentDirectory(canonFolderStore.getCanonFolder().toFile());
            }
            if (chooser.showOpenDialog(ConfigurationPanel.this) != JFileChooser.APPROVE_OPTION) return;
            java.nio.file.Path selected = chooser.getSelectedFile().toPath().toAbsolutePath();
            canonFolderStore.setCanonFolder(selected);
        });

        return verticalPanel(pathLabel, browseButton);
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
