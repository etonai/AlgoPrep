package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.problem.SelectedProblemModel;

import javax.swing.*;
import java.awt.*;

/**
 * The bottom area shared by the Problems tab and the study list tab: a message line, then Upload,
 * Studied and Clear Studied Tag, then the selected problem's name. The buttons are the shared ones,
 * bound to {@link UploadController} and {@link StudiedController}, and act on the <em>selected</em>
 * problem, not the row that is merely highlighted, which is why its name is shown below them.
 */
final class SelectionActions {

    private SelectionActions() {}

    static JPanel build(JLabel message, SelectedProblemModel selection, UploadController uploads,
                        StudiedController studiedControls) {
        JButton upload = new JButton("Upload");
        uploads.bind(upload);
        JButton studiedButton = new JButton("Studied");
        studiedControls.bindStudied(studiedButton);
        JButton clearButton = new JButton("Clear Studied Tag");
        studiedControls.bindClear(clearButton);

        JLabel selected = new JLabel(selection.describe());
        selection.addListener(() -> UiThread.run(() -> selected.setText(selection.describe())));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(upload);
        buttons.add(studiedButton);
        buttons.add(clearButton);

        JPanel selectedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selectedRow.add(selected);

        JPanel rows = new JPanel(new BorderLayout(0, 4));
        rows.add(buttons, BorderLayout.NORTH);
        rows.add(selectedRow, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(message, BorderLayout.NORTH);
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }
}
