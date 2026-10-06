package com.chatstory.theme;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class NativeThemeApplier {

    private static final Color DARK_BG = new Color(34, 36, 40);
    private static final Color DARK_PANEL = new Color(42, 45, 50);
    private static final Color DARK_FIELD = new Color(28, 30, 34);
    private static final Color DARK_TEXT = new Color(232, 234, 237);
    private static final Color DARK_MUTED = new Color(176, 181, 189);
    private static final Color DARK_BORDER = new Color(76, 82, 90);
    private static final Color DARK_TAB_SELECTED = new Color(70, 75, 85);

    private static final Color LIGHT_BG = new Color(242, 242, 242);
    private static final Color LIGHT_PANEL = new Color(250, 250, 250);
    private static final Color LIGHT_FIELD = Color.WHITE;
    private static final Color LIGHT_TEXT = new Color(30, 30, 30);
    private static final Color LIGHT_MUTED = new Color(75, 75, 75);
    private static final Color LIGHT_BORDER = new Color(190, 190, 190);
    private static final Color LIGHT_TAB_SELECTED = Color.WHITE;

    public void apply(Window window, NativeTheme theme) {
        NativeTheme resolved = theme == null ? NativeTheme.DARK : theme;
        applyTabUiDefaults(resolved);
        applyTo(window, resolved);
        SwingUtilities.updateComponentTreeUI(window);
        applyTo(window, resolved);
        window.invalidate();
        window.validate();
        window.repaint();
    }

    private void applyTabUiDefaults(NativeTheme theme) {
        if (theme == NativeTheme.LIGHT) {
            UIManager.put("TabbedPane.selected", LIGHT_TAB_SELECTED);
            UIManager.put("TabbedPane.foreground", LIGHT_TEXT);
            UIManager.put("TabbedPane.selectedForeground", LIGHT_TEXT);
        } else {
            UIManager.put("TabbedPane.selected", DARK_TAB_SELECTED);
            UIManager.put("TabbedPane.foreground", DARK_TEXT);
            UIManager.put("TabbedPane.selectedForeground", DARK_TEXT);
        }
    }

    private void applyTo(Component component, NativeTheme theme) {
        if (component == null) return;

        Colors colors = colors(theme);
        if (component instanceof JTextArea || component instanceof JTextField
                || component instanceof JEditorPane) {
            component.setBackground(colors.field);
            component.setForeground(colors.text);
        } else if (component instanceof AbstractButton) {
            component.setBackground(colors.panel);
            component.setForeground(colors.text);
        } else if (component instanceof JLabel) {
            component.setBackground(colors.panel);
            component.setForeground(colors.muted);
        } else if (component instanceof JTabbedPane) {
            component.setBackground(colors.panel);
            component.setForeground(colors.text);
        } else if (component instanceof JPanel || component instanceof JScrollPane
                || component instanceof JViewport || component instanceof JSplitPane) {
            component.setBackground(colors.panel);
            component.setForeground(colors.text);
        } else if (component instanceof JFrame) {
            component.setBackground(colors.background);
        }

        if (component instanceof JComponent jComponent) {
            jComponent.setOpaque(!(jComponent instanceof JButton));
            if (jComponent.getBorder() instanceof TitledBorder titledBorder) {
                titledBorder.setTitleColor(colors.text);
                titledBorder.setBorder(BorderFactory.createLineBorder(colors.border));
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyTo(child, theme);
            }
        }
    }

    private Colors colors(NativeTheme theme) {
        if (theme == NativeTheme.LIGHT) {
            return new Colors(LIGHT_BG, LIGHT_PANEL, LIGHT_FIELD, LIGHT_TEXT, LIGHT_MUTED, LIGHT_BORDER);
        }
        return new Colors(DARK_BG, DARK_PANEL, DARK_FIELD, DARK_TEXT, DARK_MUTED, DARK_BORDER);
    }

    private record Colors(Color background, Color panel, Color field, Color text,
                          Color muted, Color border) {
    }
}
