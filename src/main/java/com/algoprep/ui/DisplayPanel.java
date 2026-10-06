package com.algoprep.ui;

import javax.swing.*;

/**
 * Left pane: three fixed, read-only tabs (Problem, Notes, My Notes). This cycle shows placeholders
 * only. Real content and Markdown rendering arrive in DC3.
 */
public class DisplayPanel extends JTabbedPane {

    public DisplayPanel() {
        addTab("Problem", placeholder("No problem selected."));
        addTab("Notes", placeholder("No problem selected."));
        addTab("My Notes", placeholder("No problem selected."));
    }

    private static JComponent placeholder(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return new JScrollPane(area);
    }
}
