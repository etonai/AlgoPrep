package com.algoprep.ui;

import com.algoprep.display.FontScaleModel;
import com.algoprep.theme.NativeThemeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

/** Text size changes in the Markdown view: the text is kept, it gets bigger, and the place is kept. */
class MarkdownViewScaleTest {

    private FontScaleModel scale;
    private MarkdownView view;
    private JFrame frame;

    private static String longDocument() {
        StringBuilder sb = new StringBuilder("# A long problem\n\n");
        for (int i = 1; i <= 300; i++) {
            sb.append("Paragraph ").append(i).append(" has some words so that it takes a line or two.\n\n");
        }
        return sb.toString();
    }

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    /** Lets the size change and its deferred scroll restore run, then lays the window out. */
    private void settle() throws Exception {
        edt(() -> { });
        edt(() -> { });
        edt(() -> frame.validate());
        edt(() -> { });
    }

    @BeforeEach
    void setUp() throws Exception {
        scale = new FontScaleModel();
        edt(() -> {
            view = new MarkdownView(new NativeThemeModel(), scale);
            frame = new JFrame();
            frame.add(view);
            frame.setSize(400, 300);
            frame.addNotify();
            view.showMarkdown(longDocument());
            frame.validate();
        });
        settle();
    }

    private int preferredHeight() throws Exception {
        int[] h = new int[1];
        edt(() -> h[0] = view.editorPane().getPreferredSize().height);
        return h[0];
    }

    private int topOffset() throws Exception {
        int[] o = new int[1];
        edt(() -> {
            JViewport vp = (JViewport) view.editorPane().getParent();
            o[0] = view.editorPane().viewToModel2D(vp.getViewPosition());
        });
        return o[0];
    }

    /**
     * The view returns to the reading position shortly after the new layout exists, not in the same
     * instant, so wait (up to 5 seconds) for the same text to be back at the top and return what is
     * there when it is, or when time runs out.
     */
    private int waitForTopNear(int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        int current;
        do {
            settle();
            current = topOffset();
            if (Math.abs(current - expected) <= 150) {
                break;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() < deadline);
        return current;
    }

    private void scrollTo(String marker) throws Exception {
        edt(() -> {
            try {
                int offset = view.renderedText().indexOf(marker);
                assertTrue(offset > 0, "marker found");
                Rectangle2D r = view.editorPane().modelToView2D(offset);
                ((JViewport) view.editorPane().getParent()).setViewPosition(new Point(0, (int) r.getY()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        settle();
    }

    @Test
    void largerTextTakesMoreRoomAndSmallerTextTakesLess() throws Exception {
        int normal = preferredHeight();

        scale.increase();
        scale.increase();
        settle();
        int larger = preferredHeight();

        scale.decrease();
        scale.decrease();
        scale.decrease();
        scale.decrease();
        settle();
        int smaller = preferredHeight();

        assertTrue(larger > normal, "larger " + larger + " vs normal " + normal);
        assertTrue(smaller < normal, "smaller " + smaller + " vs normal " + normal);
    }

    @Test
    void theTextIsKeptWhenTheSizeChanges() throws Exception {
        String before = view.renderedText();

        scale.increase();
        settle();

        assertEquals(before, view.renderedText());
    }

    @Test
    void theReadingPositionIsKeptWhenTheSizeChanges() throws Exception {
        scrollTo("Paragraph 150 ");
        int before = topOffset();
        assertTrue(before > 1000, "scrolled well down the document: " + before);

        scale.increase();
        scale.increase();
        int after = waitForTopNear(before);

        assertEquals(before, after, 150, "the same text is still at the top (" + before + " vs " + after + ")");
        assertTrue(after > 1000, "did not jump back to the top");
    }

    @Test
    void theReadingPositionIsKeptWhenShrinkingToo() throws Exception {
        scrollTo("Paragraph 200 ");
        int before = topOffset();

        scale.decrease();
        int after = waitForTopNear(before);

        assertEquals(before, after, 150);
    }
}
