package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import java.awt.*;

/**
 * Settings tab. This cycle has the theme only. The PROBLEMS, HOME and staging folders are added in
 * later cycles (Plan section 6.3).
 */
public class SettingsPanel extends JPanel {

    public SettingsPanel(NativeThemeModel themeModel) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(sectionLabel("Theme"));
        content.add(themeControls(themeModel));

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

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dark.setAlignmentX(Component.LEFT_ALIGNMENT);
        light.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(dark);
        panel.add(light);
        return panel;
    }
}
