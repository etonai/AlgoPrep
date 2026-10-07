package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.problem.Problem;
import com.algoprep.problem.ProblemCatalog;
import com.algoprep.problem.ProblemFilter;
import com.algoprep.problem.ProblemRowText;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedStore;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Problems tab (Plan section 6.2): a filter, a list of {@code number - name} rows and Refresh.
 * Double-click or Enter selects a problem. A single click only highlights it. Only the number and
 * name are shown, never individual files.
 */
public class ProblemsPanel extends JPanel {

    private final ProblemWorkspace workspace;
    private final JTextField filterField = new JTextField();
    private final DefaultListModel<Problem> listModel = new DefaultListModel<>();
    private final JList<Problem> list = new JList<>(listModel);
    private final JLabel message = new JLabel(" ");

    public ProblemsPanel(ProblemWorkspace workspace, UploadController uploads,
                         StudiedController studiedControls, StudiedStore studied) {
        super(new BorderLayout(0, 6));
        this.workspace = workspace;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        filterField.setToolTipText("Filter by number or name");
        JButton refresh = new JButton("Refresh");
        refresh.setToolTipText("Rescan the PROBLEMS directory");
        refresh.addActionListener(e -> {
            workspace.refresh();
            studied.reload(); // picks up hand edits to the studied file
        });

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.add(new JLabel("Filter:"), BorderLayout.WEST);
        top.add(filterField, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RowRenderer(workspace.selection(), studied));
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
        workspace.catalog().addListener(() -> UiThread.run(this::rebuild));
        // Repaint so the "selected" marker follows the selection model, wherever it was changed
        workspace.selection().addListener(() -> UiThread.run(list::repaint));
        // Repaint so the STUDIED tags follow the studied file. Reload it whenever the tab is shown,
        // so edits made outside AlgoPrep appear without a file watcher.
        studied.addListener(() -> UiThread.run(list::repaint));
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                studied.reload();
                UiThread.run(this::highlightSelected);
            }
        });

        rebuild();
    }

    /** Highlights (as if clicked) and scrolls to the selected problem's row, if there is one and it is shown. */
    void highlightSelected() {
        workspace.selection().selectedKey().ifPresent(key -> {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).key().equalsIgnoreCase(key)) {
                    list.setSelectedIndex(i);
                    list.ensureIndexIsVisible(i);
                    return;
                }
            }
        });
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
        if (index >= 0 && index < listModel.size()) {
            workspace.selection().select(listModel.get(index));
        }
    }

    /** Refills the list from the catalog and the filter, keeping the highlighted row if it is still shown. */
    private void rebuild() {
        Problem highlighted = list.getSelectedValue();
        String highlightedKey = highlighted == null ? null : highlighted.key();

        ProblemCatalog catalog = workspace.catalog();
        List<Problem> all = catalog.problems();
        String query = filterField.getText();

        listModel.clear();
        int restoreIndex = -1;
        for (Problem p : all) {
            if (ProblemFilter.matches(p, query)) {
                listModel.addElement(p);
                if (p.key().equals(highlightedKey)) {
                    restoreIndex = listModel.size() - 1;
                }
            }
        }
        if (restoreIndex >= 0) {
            list.setSelectedIndex(restoreIndex);
        }

        updateMessage(all.size());
    }

    private void updateMessage(int total) {
        String dir = workspace.directoryText();
        String error = workspace.catalog().lastError();
        String text;
        if (dir == null || dir.isBlank()) {
            text = "No PROBLEMS directory set. Choose one in Settings.";
        } else if (error != null) {
            text = total > 0 ? error + " (showing the previous list)" : error;
        } else if (total == 0) {
            text = "No problems found in the PROBLEMS directory.";
        } else if (listModel.isEmpty()) {
            text = "No problems match the filter.";
        } else if (listModel.size() == total) {
            text = total + " problem(s)";
        } else {
            text = listModel.size() + " of " + total + " problem(s)";
        }
        message.setText(text);
        message.setToolTipText(text);
    }

    /** Marks the row of the currently selected problem (distinct from the highlighted row), and the studied ones. */
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
            if (value instanceof Problem p) {
                boolean current = selection.selectedKey().map(k -> k.equalsIgnoreCase(p.key())).orElse(false);
                setText(ProblemRowText.of(p.displayName(), current, studied.studiedOn(p.key())));
                setFont(getFont().deriveFont(current ? Font.BOLD : Font.PLAIN));
            }
            return this;
        }
    }
}
