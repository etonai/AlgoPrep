package com.algoprep.display;

import com.algoprep.theme.NativeTheme;

import java.util.List;

/**
 * CSS rules for the Markdown view, one set per theme (Plan section 7.1). Pure: no Swing.
 *
 * <p>The background and text colors deliberately repeat the field and text colors in
 * {@code NativeThemeApplier}, which sets the same colors on every {@code JEditorPane}, so the pane
 * does not look mismatched. That class is copied ChatStory code and is left alone, so keep these
 * in step with it by hand.
 *
 * <p>Swing's renderer supports only a small CSS 1 subset, so these rules are checked by eye.
 */
public final class MarkdownStyles {

    private record Palette(String background, String text, String strong, String muted,
                           String codeBackground, String link) { }

    // NativeThemeApplier: DARK_FIELD (28,30,34), DARK_TEXT (232,234,237), DARK_MUTED (176,181,189)
    private static final Palette DARK = new Palette(
            "#1c1e22", "#e8eaed", "#ffffff", "#b0b5bd", "#2a2d32", "#4fc3f7");

    // NativeThemeApplier: LIGHT_FIELD (white), LIGHT_TEXT (30,30,30), LIGHT_MUTED (75,75,75)
    private static final Palette LIGHT = new Palette(
            "#ffffff", "#1e1e1e", "#000000", "#4b4b4b", "#eeeeee", "#0b57d0");

    private MarkdownStyles() {}

    /** The page background for the theme, for the pane itself. */
    public static String background(NativeTheme theme) {
        return palette(theme).background();
    }

    /** CSS rules to add, in order, to a fresh {@code StyleSheet}. */
    public static List<String> rules(NativeTheme theme) {
        Palette p = palette(theme);
        return List.of(
                "body { font-family: sans-serif; font-size: 12pt; color: " + p.text()
                        + "; background-color: " + p.background() + "; margin: 8px; }",
                "h1 { font-size: 18pt; color: " + p.strong() + "; }",
                "h2 { font-size: 16pt; color: " + p.strong() + "; }",
                "h3 { font-size: 14pt; color: " + p.strong() + "; }",
                "h4, h5, h6 { font-size: 12pt; color: " + p.strong() + "; }",
                "strong, b { color: " + p.strong() + "; }",
                "em, i { color: " + p.muted() + "; }",
                "code { font-family: monospaced; font-size: 11pt; background-color: "
                        + p.codeBackground() + "; }",
                "pre { font-family: monospaced; font-size: 11pt; background-color: "
                        + p.codeBackground() + "; margin: 4px 0; padding: 4px; }",
                "blockquote { color: " + p.muted() + "; margin-left: 16px; }",
                "ul, ol { margin-left: 20px; }",
                "li { margin-top: 2px; }",
                "a { color: " + p.link() + "; }",
                "p { margin-top: 4px; margin-bottom: 4px; }",
                "th { color: " + p.strong() + "; background-color: " + p.codeBackground() + "; }",
                "td, th { color: " + p.text() + "; }");
    }

    private static Palette palette(NativeTheme theme) {
        return theme == NativeTheme.LIGHT ? LIGHT : DARK;
    }
}
