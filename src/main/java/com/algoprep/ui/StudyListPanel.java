package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedStore;
import com.algoprep.studylist.StudyListFilter;
import com.algoprep.studylist.StudyListModel;
import com.algoprep.studylist.StudyListRow;
import com.algoprep.studylist.StudyListRowText;
import com.algoprep.studylist.StudyListSummary;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The study list tab: the problems of the study list file, in the file's order, with the difficulty
 * and time from the file. It works like the Problems tab: a filter, a list, Refresh, and
 * double-click or Enter to select a problem. A single click only highlights it. Rows show the same
 * {@code (selected)} and {@code (STUDIED date)} tags, and a problem with no files in the PROBLEMS
 * directory is shown as NOT FOUND and cannot be selected.
 *
 * <p>The tab owns no selection or studied state. It observes the shared models and uses the shared
 * Upload and Studied buttons. Nothing here depends on the browser.
 */
public class StudyListPanel extends JPanel {

    private static final Color NOT_FOUND_COLOR = new Color(0x80, 0x80, 0x80);

    private final StudyListModel model;
    private final ProblemWorkspace workspace;
    private final StudiedStore studied;
    private final JTextField filterField = new JTextField();
    private final DefaultListModel<StudyListRow> listModel = new DefaultListModel<>();
    private final JList<StudyListRow> list = new JList<>(listModel);
    private final JLabel message = new JLabel(" ");

    public StudyListPanel(StudyListModel model, ProblemWorkspace workspace, UploadController uploads,
                          StudiedController studiedControls, StudiedStore studied) {
        super(new BorderLayout(0, 6));
        this.model = model;
        this.workspace = workspace;
        this.studied = studied;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        filterField.setToolTipText("Filter by number, name, difficulty or time");
        JButton refresh = new JButton("Refresh");
        refresh.setToolTipText("Rescan the PROBLEMS directory and re-read the study list file");
        refresh.addActionListener(e -> {
            workspace.refresh();
            model.reload();
            studied.reload();
        });

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.add(new JLabel("Filter:"), BorderLayout.WEST);
        top.add(filterField, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RowRenderer(workspace.selection(), studied));
        ToolTipManager.sharedInstance().registerComponent(list); // each row has its full text as a tooltip
        installActivation();

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(SelectionActions.build(message, workspace.selection(), uploads, studiedControls),
                BorderLayout.SOUTH);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { rebuild(); }
            @Override public void removeUpdate(DocumentEvent e)  { rebuild(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuild(); }
        });
        model.addListener(() -> UiThread.run(this::rebuild));
        // Repaint so the markers follow the selection and the studied file, wherever they change
        workspace.selection().addListener(() -> UiThread.run(list::repaint));
        studied.addListener(() -> UiThread.run(() -> {
            list.repaint();
            updateMessage();
        }));
        // Re-read everything whenever the tab is shown, so outside edits appear without a watcher
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                workspace.catalog().refresh(); // quietly: no status message each time the tab is shown
                model.reloadQuietly();
                studied.reload();
            }
        });

        rebuild();
    }

    /** Double-click and Enter select the problem. A single click only highlights it. */
    private void installActivation() {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(e)) return;
                int index = list.locationToIndex(e.getPoint());
                Rectangle cell = index >= 0 ? list.getCellBounds(index, index) : null;
                if (cell != null && cell.contains(e.getPoint())) {
                    activate(index);
                }
            }
        });
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
        list.getActionMap().put("activate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                activate(list.getSelectedIndex());
            }
        });
    }

    private void activate(int index) {
        if (index < 0 || index >= listModel.size()) {
            return;
        }
        StudyListRow row = listModel.get(index);
        if (row.problem().isPresent()) {
            workspace.selection().select(row.problem().get());
        } else {
            message.setText(row.entry().key() + " has no files in the PROBLEMS directory.");
            message.setToolTipText(message.getText());
        }
    }

    /** Refills the list from the model and the filter, keeping the highlighted row if still shown. */
    private void rebuild() {
        StudyListRow highlighted = list.getSelectedValue();
        String highlightedKey = highlighted == null ? null : highlighted.entry().key();
        String query = filterField.getText();

        listModel.clear();
        int restoreIndex = -1;
        for (StudyListRow row : model.rows()) {
            if (StudyListFilter.matches(row, query)) {
                listModel.addElement(row);
                if (restoreIndex < 0 && row.entry().key().equals(highlightedKey)) {
                    restoreIndex = listModel.size() - 1;
                }
            }
        }
        if (restoreIndex >= 0) {
            list.setSelectedIndex(restoreIndex);
        }
        updateMessage();
    }

    private void updateMessage() {
        String text;
        if (model.error() != null) {
            text = model.error();
        } else {
            List<StudyListRow> all = model.rows();
            int studiedCount = 0;
            int notFound = 0;
            for (StudyListRow row : all) {
                if (row.problem().isEmpty()) {
                    notFound++;
                } else if (studied.studiedOn(row.problem().get().key()).isPresent()) {
                    studiedCount++;
                }
            }
            text = all.isEmpty() || listModel.size() > 0 || filterField.getText().isBlank()
                    ? StudyListSummary.text(all.size(), listModel.size(), studiedCount, notFound)
                    : "No problems match the filter.";
            if (model.skipped() > 0) {
                text += " (" + model.skipped() + " line(s) skipped)";
            }
        }
        message.setText(text);
        message.setToolTipText(text);
    }

    /** The visible row texts, for tests. */
    List<String> rowTexts() {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            StudyListRow row = listModel.get(i);
            texts.add(textOf(row, workspace.selection(), studied));
        }
        return texts;
    }

    private static boolean isSelected(StudyListRow row, SelectedProblemModel selection) {
        return row.problem()
                .map(p -> selection.selectedKey().map(k -> k.equalsIgnoreCase(p.key())).orElse(false))
                .orElse(false);
    }

    private static String textOf(StudyListRow row, SelectedProblemModel selection, StudiedStore studied) {
        return StudyListRowText.of(row, isSelected(row, selection),
                row.problem().flatMap(p -> studied.studiedOn(p.key())));
    }

    /** Marks the selected problem's row in bold, and shows rows that were not found in a muted color. */
    private static final class RowRenderer extends DefaultListCellRenderer {
        private final SelectedProblemModel selection;
        private final StudiedStore studied;

        RowRenderer(SelectedProblemModel selection, StudiedStore studied) {
            this.selection = selection;
            this.studied = studied;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof StudyListRow row) {
                String text = textOf(row, selection, studied);
                setText(text);
                setToolTipText(text);
                setFont(getFont().deriveFont(isSelected(row, selection) ? Font.BOLD : Font.PLAIN));
                if (!row.isFound() && !isSelected) {
                    setForeground(NOT_FOUND_COLOR);
                }
            }
            return this;
        }
    }
}
