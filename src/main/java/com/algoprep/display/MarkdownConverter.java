package com.algoprep.display;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts Markdown to the HTML that Swing's {@code JEditorPane} can show (Plan section 7.1). Pure:
 * no Swing.
 *
 * <p>The files can contain anything, so the output is locked down:
 * <ul>
 *   <li>raw HTML is escaped and shown as text;</li>
 *   <li>URLs are sanitized, so {@code javascript:} links are dropped;</li>
 *   <li>images become a text placeholder, so {@code JEditorPane} never fetches a URL;</li>
 *   <li>tables get border attributes, because Swing draws none from CSS alone.</li>
 * </ul>
 */
public final class MarkdownConverter {

    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .attributeProviderFactory(context -> new TableAttributes())
            .nodeRendererFactory(ImagePlaceholderRenderer::new)
            .build();

    private MarkdownConverter() {}

    /** Converts Markdown to an HTML fragment (no {@code <html>} or {@code <body>} wrapper). */
    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        return RENDERER.render(PARSER.parse(markdown));
    }

    /** Escapes text for use inside HTML, for messages that are not Markdown. */
    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static final class TableAttributes implements AttributeProvider {
        @Override
        public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
            if ("table".equals(tagName)) {
                attributes.put("border", "1");
                attributes.put("cellpadding", "4");
                attributes.put("cellspacing", "0");
            }
        }
    }

    /** Writes {@code [image: alt]} in place of an image. */
    private static final class ImagePlaceholderRenderer implements NodeRenderer {
        private final HtmlWriter html;

        ImagePlaceholderRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(Image.class);
        }

        @Override
        public void render(Node node) {
            StringBuilder alt = new StringBuilder();
            node.accept(new AbstractVisitor() {
                @Override
                public void visit(Text text) {
                    alt.append(text.getLiteral());
                }

                @Override
                public void visit(Code code) {
                    alt.append(code.getLiteral());
                }
            });
            String label = alt.toString().trim();
            html.text(label.isEmpty() ? "[image]" : "[image: " + label + "]");
        }
    }
}
