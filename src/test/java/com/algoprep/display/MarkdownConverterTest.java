package com.algoprep.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownConverterTest {

    private static String html(String md) {
        return MarkdownConverter.toHtml(md);
    }

    @Test
    void emptyInputGivesEmptyOutput() {
        assertEquals("", html(""));
        assertEquals("", html(null));
    }

    @Test
    void headingsListsAndEmphasisConvert() {
        String out = html("# Title\n\n- one\n- two\n\nSome **bold** and *italic* text.\n");

        assertTrue(out.contains("<h1>Title</h1>"), out);
        assertTrue(out.contains("<ul>") && out.contains("<li>one</li>"), out);
        assertTrue(out.contains("<strong>bold</strong>"), out);
        assertTrue(out.contains("<em>italic</em>"), out);
    }

    @Test
    void fencedCodeBecomesPreCode() {
        String out = html("```java\nint x = 1;\n```\n");

        assertTrue(out.contains("<pre><code"), out);
        assertTrue(out.contains("int x = 1;"), out);
    }

    @Test
    void inlineCodeIsEscaped() {
        assertTrue(html("Use `a < b` here").contains("<code>a &lt; b</code>"));
    }

    @Test
    void rawHtmlBlocksAreEscapedNotInterpreted() {
        String out = html("<script>alert(1)</script>\n\nText with <b>raw</b> html\n");

        assertFalse(out.contains("<script"), out);
        assertFalse(out.contains("<b>"), out);
        assertTrue(out.contains("&lt;script&gt;"), out);
        assertTrue(out.contains("&lt;b&gt;raw&lt;/b&gt;"), out);
    }

    @Test
    void rawImgTagIsEscaped() {
        String out = html("<img src=\"http://example.test/x.png\">");

        assertFalse(out.contains("<img"), out);
    }

    @Test
    void markdownImagesBecomeATextPlaceholderAndNeverAnImgTag() {
        String out = html("![diagram of a tree](http://example.test/tree.png)");

        assertFalse(out.contains("<img"), out);
        assertFalse(out.contains("example.test"), out);
        assertTrue(out.contains("[image: diagram of a tree]"), out);
    }

    @Test
    void imageWithoutAltText() {
        assertTrue(html("![](http://example.test/x.png)").contains("[image]"));
    }

    @Test
    void imageAltTextIsEscaped() {
        String out = html("![<b>x</b>](http://example.test/x.png)");

        assertFalse(out.contains("<b>"), out);
    }

    @Test
    void javascriptLinksAreNotEmitted() {
        String out = html("[click](javascript:alert(1))");

        assertFalse(out.toLowerCase().contains("javascript:"), out);
        assertTrue(out.contains("click"), out);
    }

    @Test
    void ordinaryLinksKeepTheirText() {
        String out = html("[docs](https://example.test/docs)");

        assertTrue(out.contains(">docs</a>"), out);
    }

    @Test
    void gfmTablesGetBorderAttributes() {
        String out = html("| a | b |\n|---|---|\n| 1 | 2 |\n");

        assertTrue(out.contains("<table"), out);
        assertTrue(out.contains("border=\"1\""), out);
        assertTrue(out.contains("cellpadding=\"4\""), out);
        assertTrue(out.contains("<th>a</th>") || out.contains("<th"), out);
        assertTrue(out.contains("<td>1</td>") || out.contains("<td"), out);
    }

    @Test
    void blockquotesConvert() {
        assertTrue(html("> quoted\n").contains("<blockquote>"));
    }

    @Test
    void nonAsciiTextSurvives() {
        assertTrue(html("Größe → ✓ 日本語").contains("Größe → ✓ 日本語"));
    }

    @Test
    void escapeHelperEscapesTheFourCharacters() {
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot;", MarkdownConverter.escape("a & b <c> \"d\""));
    }
}
