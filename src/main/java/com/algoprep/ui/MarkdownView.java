package com.algoprep.ui;

import com.algoprep.UiThread;
import com.algoprep.display.FontScaleModel;
import com.algoprep.display.MarkdownConverter;
import com.algoprep.display.MarkdownStyles;
import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Read-only Markdown view: a {@code JEditorPane} with an {@code HTMLEditorKit} and a per-theme
 * {@code StyleSheet}, modeled on ChatStory's {@code OutputPanel} (Plan section 7.1).
 *
 * <p>There is no hyperlink listener, so links are not clickable, and the converter turns images
 * into text, so nothing is ever fetched. Text stays selectable so the user can copy from it.
 */
public class MarkdownView extends JPanel {

    private enum Kind { EMPTY, MARKDOWN, MESSAGE }

    /** How hard to try to return to the reading position after a size change (about a second). */
    private static final int RESTORE_ATTEMPTS = 25;
    private static final int RESTORE_RETRY_MS = 40;
    /** How many characters off still counts as "the same place". */
    private static final int RESTORE_TOLERANCE = 40;

    private final JEditorPane editor = new JEditorPane();
    private final JScrollPane scroll = new JScrollPane(editor);
    private final NativeThemeModel themeModel;
    private final FontScaleModel fontScale;

    /** The text offset a size change is still scrolling back to, or -1 when none is in progress. */
    private int restoreTarget = -1;
    /** Bumped by each size change, so an older return to the reading position stops. */
    private int restoreGeneration;

    private Kind kind = Kind.EMPTY;
    private String content = "";

    /** A view at the normal text size that never changes. */
    public MarkdownView(NativeThemeModel themeModel) {
        this(themeModel, new FontScaleModel());
    }

    public MarkdownView(NativeThemeModel themeModel, FontScaleModel fontScale) {
        super(new BorderLayout());
        this.themeModel = themeModel;
        this.fontScale = fontScale;

        editor.setEditable(false);
        add(scroll, BorderLayout.CENTER);

        applyKit(themeModel.current());
        // Deferred, so it runs after NativeThemeApplier has recolored and refreshed the whole
        // window (it does that on the same theme change) and our styling is not undone by it.
        themeModel.addListener((previous, current) -> SwingUtilities.invokeLater(this::restyle));
        fontScale.addListener(() -> UiThread.run(this::rescale));
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

    /**
     * Re-renders at a new text size, keeping the reading position. Re-rendering replaces the
     * document, which would otherwise jump back to the top, so the text at the top of the visible
     * area is remembered by its offset (the text itself is unchanged, only its size) and scrolled
     * back to once the new layout exists.
     */
    private void rescale() {
        // While an earlier size change is still returning to the reading position, the viewport
        // is not showing it (the new document starts at the top), so keep that earlier target
        // instead of reading the position again. Otherwise two quick presses would lose the place.
        int offset = restoreTarget >= 0
                ? restoreTarget
                : Math.max(0, editor.viewToModel2D(scroll.getViewport().getViewPosition()));
        restoreTarget = offset;
        int generation = ++restoreGeneration;

        applyKit(themeModel.current());
        render();

        restorePosition(offset, generation, RESTORE_ATTEMPTS);
    }

    /**
     * Scrolls so that the text at {@code offset} is at the top. The new layout is not final right
     * after the text is replaced, so the first attempt can scroll to the wrong place (the document
     * is still shorter than it will be). So it checks which text is at the top afterwards, and tries
     * again a moment later until the right text is there, or it gives up.
     */
    private void restorePosition(int offset, int generation, int attemptsLeft) {
        SwingUtilities.invokeLater(() -> {
            if (generation != restoreGeneration) {
                return; // a newer size change has taken over
            }
            JViewport viewport = scroll.getViewport();
            try {
                Rectangle2D target = editor.modelToView2D(offset);
                int y = target == null ? 0 : (int) target.getY();
                viewport.setViewPosition(new Point(0, Math.max(0, y)));
            } catch (BadLocationException e) {
                viewport.setViewPosition(new Point(0, 0));
                restoreTarget = -1;
                return;
            }
            int now = editor.viewToModel2D(viewport.getViewPosition());
            if (Math.abs(now - offset) > RESTORE_TOLERANCE && attemptsLeft > 0) {
                Timer retry = new Timer(RESTORE_RETRY_MS,
                        e -> restorePosition(offset, generation, attemptsLeft - 1));
                retry.setRepeats(false);
                retry.start();
            } else {
                restoreTarget = -1; // back in place, or gave up: the viewport is the truth again
            }
        });
    }

    /** Builds a fresh style sheet. The shared default sheet of HTMLEditorKit is never modified. */
    private void applyKit(NativeTheme theme) {
        StyleSheet styles = new StyleSheet();
        styles.addStyleSheet(new HTMLEditorKit().getStyleSheet());
        for (String rule : MarkdownStyles.rules(theme, fontScale.percent())) {
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
        } catch (BadLocationException e) {
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
