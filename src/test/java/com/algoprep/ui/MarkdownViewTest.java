package com.algoprep.ui;

import com.algoprep.theme.NativeTheme;
import com.algoprep.theme.NativeThemeModel;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the Swing path without a window. Looks are checked by eye in the manual phase. */
class MarkdownViewTest {

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static MarkdownView view(NativeThemeModel model) throws Exception {
        MarkdownView[] holder = new MarkdownView[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new MarkdownView(model));
        return holder[0];
    }

    private static void onEdt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    @Test
    void rendersMarkdownText() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMarkdown("# Title\n\nSome **bold** text\n\n- one\n- two\n"));

        String text = v.renderedText();
        assertTrue(text.contains("Title"), text);
        assertTrue(text.contains("bold"), text);
        assertTrue(text.contains("one") && text.contains("two"), text);
    }

    @Test
    void rawHtmlIsShownAsTextNotInterpreted() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMarkdown("<b>not bold</b> and <script>x()</script>"));

        String text = v.renderedText();
        assertTrue(text.contains("<b>not bold</b>"), text);
        assertTrue(text.contains("<script>x()</script>"), text);
    }

    @Test
    void imageShowsAsPlaceholderText() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMarkdown("![a tree](http://example.test/t.png)"));

        assertTrue(v.renderedText().contains("[image: a tree]"), v.renderedText());
    }

    @Test
    void tableAndCodeRenderWithoutError() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMarkdown("| a | b |\n|---|---|\n| 1 | 2 |\n\n```\n"
                + "x".repeat(400) + "\n```\n"));

        String text = v.renderedText();
        assertTrue(text.contains("a") && text.contains("2"), text);
        assertTrue(text.contains("x".repeat(400)), "long code line is kept whole");
    }

    @Test
    void messageIsShownAsPlainTextAndEscaped() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMessage("No <saved> notes. *not bold*"));

        String text = v.renderedText();
        assertTrue(text.contains("No <saved> notes. *not bold*"), text);
    }

    @Test
    void showingNewContentReplacesTheOld() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        onEdt(() -> v.showMarkdown("first"));
        onEdt(() -> v.showMessage("second"));

        assertFalse(v.renderedText().contains("first"));
        assertTrue(v.renderedText().contains("second"));
    }

    @Test
    void themeChangeKeepsTheContentAndRestylesThePane() throws Exception {
        NativeThemeModel model = new NativeThemeModel();
        MarkdownView v = view(model);
        onEdt(() -> v.showMarkdown("# Kept"));
        Color dark = v.editorPane().getBackground();

        onEdt(() -> model.setTheme(NativeTheme.LIGHT));
        flushEdt(); // the restyle is deferred with invokeLater

        assertTrue(v.renderedText().contains("Kept"), v.renderedText());
        assertNotEquals(dark, v.editorPane().getBackground());
        assertEquals(Color.WHITE, v.editorPane().getBackground());
    }

    @Test
    void paneIsReadOnlyAndHasNoHyperlinkListener() throws Exception {
        MarkdownView v = view(new NativeThemeModel());

        assertFalse(v.editorPane().isEditable());
        assertEquals(0, v.editorPane().getHyperlinkListeners().length);
    }
}
