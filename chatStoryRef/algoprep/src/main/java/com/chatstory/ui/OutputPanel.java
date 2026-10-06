package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.bridge.CorrectionType;
import com.chatstory.canon.CanonStore;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class OutputPanel extends JPanel {

    private static final Color BG = new Color(28, 30, 34);
    private static final Color FG = new Color(232, 234, 237);

    private final JEditorPane editorPane = new JEditorPane();
    private final JButton addToCanonButton = new JButton("Add to Canon");
    private final JLabel responseLabel = new JLabel("Response Window");
    private final Consumer<String> onBeatRecorded;
    private String currentText = "";

    public OutputPanel(CanonStore canonStore, Consumer<String> onCanonAdded,
                       Consumer<String> onSendPrompt, Runnable beforeFocusRequest,
                       Consumer<String> onBeatRecorded) {
        super(new BorderLayout(6, 6));
        this.onBeatRecorded = onBeatRecorded;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        StyleSheet styles = new StyleSheet();
        styles.addStyleSheet(new HTMLEditorKit().getStyleSheet());
        styles.addRule("body { font-family: sans-serif; font-size: 14pt;"
                + " color: #e8eaed; background-color: #1c1e22; margin: 8px; }");
        styles.addRule("h1 { font-size: 20pt; color: #ffffff; }");
        styles.addRule("h2 { font-size: 18pt; color: #ffffff; }");
        styles.addRule("h3 { font-size: 16pt; color: #ffffff; }");
        styles.addRule("h4, h5, h6 { color: #ffffff; }");
        styles.addRule("strong, b { color: #ffffff; }");
        styles.addRule("em, i { color: #b0b5bd; }");
        styles.addRule("code { font-family: monospace; font-size: 12pt;"
                + " background-color: #2a2d32; }");
        styles.addRule("pre { font-family: monospace; font-size: 12pt;"
                + " background-color: #2a2d32; margin: 4px 0; padding: 4px; }");
        styles.addRule("blockquote { color: #b0b5bd; margin-left: 16px; }");
        styles.addRule("ul, ol { margin-left: 20px; }");
        styles.addRule("li { margin-top: 2px; }");
        styles.addRule("a { color: #4fc3f7; }");
        styles.addRule("p { margin-top: 4px; margin-bottom: 4px; }");

        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(styles);

        editorPane.setEditorKit(kit);
        editorPane.setDocument(kit.createDefaultDocument());
        editorPane.setEditable(false);
        editorPane.setBackground(BG);
        editorPane.setForeground(FG);

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(currentText);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        addToCanonButton.setEnabled(false);
        addToCanonButton.addActionListener(e -> {
            canonStore.add(currentText);
            onCanonAdded.accept(currentText);
            addToCanonButton.setText("Added!");
            addToCanonButton.setEnabled(false);
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            currentText = "";
            editorPane.setText("");
            addToCanonButton.setText("Add to Canon");
            addToCanonButton.setEnabled(true);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addToCanonButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(copyButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(responseLabel, BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(editorPane);
        add(scrollPane, BorderLayout.CENTER);

        installCorrectionMenu(onSendPrompt);
        installFocusRecovery(scrollPane, beforeFocusRequest);
    }

    private void installFocusRecovery(JScrollPane scrollPane, Runnable beforeFocusRequest) {
        MouseAdapter focusRecovery = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    beforeFocusRequest.run();
                    editorPane.requestFocusInWindow();
                    SwingUtilities.invokeLater(() -> {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        editorPane.requestFocusInWindow();
                    });
                }
            }
        };
        editorPane.addMouseListener(focusRecovery);
        scrollPane.addMouseListener(focusRecovery);
        scrollPane.getViewport().addMouseListener(focusRecovery);
    }

    private void installCorrectionMenu(Consumer<String> onSendPrompt) {
        JPopupMenu menu = new JPopupMenu();
        Map<CorrectionType, JMenuItem> items = new EnumMap<>(CorrectionType.class);
        for (CorrectionType type : CorrectionType.values()) {
            JMenuItem item = new JMenuItem(type.menuLabel());
            item.addActionListener(e -> {
                String selected = editorPane.getSelectedText();
                if (selected != null && !selected.isBlank()) {
                    onSendPrompt.accept(type.buildPrompt(selected));
                }
            });
            menu.add(item);
            items.put(type, item);
        }

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelection = editorPane.getSelectedText() != null
                        && !editorPane.getSelectedText().isBlank();
                items.values().forEach(item -> item.setEnabled(hasSelection));
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        editorPane.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(editorPane, e.getX(), e.getY());
                }
            }
        });
    }

    private String wrapHtml(String html) {
        return "<html><body>" + (html == null ? "" : html) + "</body></html>";
    }

    public void setResponse(String text) {
        setResponse(text, null, -1);
    }

    public void setResponse(String text, String html, int beatNumber) {
        System.out.println("[OutputPanel] setResponse enter thread=" + Thread.currentThread().getName()
                + " onEdt=" + SwingUtilities.isEventDispatchThread()
                + " beatNumber=" + beatNumber + " textLen=" + (text == null ? 0 : text.length()));
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("[OutputPanel] setResponse queueing on EDT");
            UiThread.run(() -> setResponse(text, html, beatNumber));
            System.out.println("[OutputPanel] setResponse queued on EDT");
            return;
        }
        currentText = text == null ? "" : text;
        if (!currentText.isBlank()) {
            System.out.println("[OutputPanel] setResponse invoking onBeatRecorded thread="
                    + Thread.currentThread().getName()
                    + " onEdt=" + SwingUtilities.isEventDispatchThread());
            onBeatRecorded.accept(currentText);
            System.out.println("[OutputPanel] setResponse onBeatRecorded returned");
        }
        String display = (html != null && !html.isBlank()) ? html : "<pre>" + currentText + "</pre>";
        UiThread.run(() -> {
            System.out.println("[OutputPanel] setResponse EDT update start thread="
                    + Thread.currentThread().getName());
            if (beatNumber >= 0) responseLabel.setText("Current Beat " + beatNumber);
            editorPane.setText(wrapHtml(display));
            editorPane.setCaretPosition(0);
            boolean hasContent = !currentText.isBlank();
            addToCanonButton.setEnabled(hasContent);
            if (hasContent) addToCanonButton.setText("Add to Canon");
            System.out.println("[OutputPanel] setResponse EDT update done");
        });
        System.out.println("[OutputPanel] setResponse exit");
    }

    public void focusResponse() {
        editorPane.requestFocusInWindow();
    }

    public void clear() {
        currentText = "";
        UiThread.run(() -> {
            responseLabel.setText("Response Window");
            editorPane.setText("");
            addToCanonButton.setText("Add to Canon");
            addToCanonButton.setEnabled(false);
        });
    }
}
