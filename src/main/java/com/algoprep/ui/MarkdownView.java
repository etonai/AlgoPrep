package com.algoprep.ui;

import com.algoprep.display.MarkdownConverter;
import com.algoprep.display.MarkdownStyles;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * Read-only Markdown view: a {@code JEditorPane} with an {@code HTMLEditorKit} and a per-theme
 * {@code StyleSheet}, modeled on ChatStory's {@code OutputPanel} (Plan section 7.1).
 *
 * <p>There is no hyperlink listener, so links are not clickable, and the converter turns images
 * into text, so nothing is ever fetched. Text stays selectable so the user can copy from it.
 */
public class MarkdownView extends JPanel {

    private enum Kind { EMPTY, MARKDOWN, MESSAGE }

    private final JEditorPane editor = new JEditorPane();
    private final NativeThemeModel themeModel;

    private Kind kind = Kind.EMPTY;
    private String content = "";

    public MarkdownView(NativeThemeModel themeModel) {
        super(new BorderLayout());
        this.themeModel = themeModel;

        editor.setEditable(false);
        add(new JScrollPane(editor), BorderLayout.CENTER);

        applyKit(themeModel.current());
        // Deferred, so it runs after NativeThemeApplier has recolored and refreshed the whole
        // window (it does that on the same theme change) and our styling is not undone by it.
        themeModel.addListener((previous, current) -> SwingUtilities.invokeLater(this::restyle));
    }

    /** Renders Markdown. Showing the same text again leaves the scroll position alone. */
    public void showMarkdown(String markdown) {
        update(Kind.MARKDOWN, markdown);
    }

    /** Shows a plain, muted message (not Markdown). */
    public void showMessage(String message) {
        update(Kind.MESSAGE, message);
    }

    private void update(Kind newKind, String newContent) {
        if (newKind == kind && newContent.equals(content)) {
            return;
        }
        kind = newKind;
        content = newContent;
        render();
        editor.setCaretPosition(0);
    }

    private void restyle() {
        applyKit(themeModel.current());
        render();
        editor.setCaretPosition(0);
    }

    /** Builds a fresh style sheet. The shared default sheet of HTMLEditorKit is never modified. */
    private void applyKit(NativeTheme theme) {
        StyleSheet styles = new StyleSheet();
        styles.addStyleSheet(new HTMLEditorKit().getStyleSheet());
        for (String rule : MarkdownStyles.rules(theme)) {
            styles.addRule(rule);
        }
        HTMLEditorKit kit = new HTMLEditorKit();
        kit.setStyleSheet(styles);
        editor.setEditorKit(kit);
        editor.setDocument(kit.createDefaultDocument());
        editor.setBackground(Color.decode(MarkdownStyles.background(theme)));
    }

    /** The text the pane currently displays, for tests. */
    String renderedText() {
        try {
            return editor.getDocument().getText(0, editor.getDocument().getLength());
        } catch (javax.swing.text.BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The pane itself, for tests that paint it. */
    JEditorPane editorPane() {
        return editor;
    }

    private void render() {
        String body = switch (kind) {
            case MARKDOWN -> MarkdownConverter.toHtml(content);
            case MESSAGE -> "<p><i>" + MarkdownConverter.escape(content) + "</i></p>";
            case EMPTY -> "";
        };
        editor.setText("<html><body>" + body + "</body></html>");
    }
}
