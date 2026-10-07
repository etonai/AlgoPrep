package com.algoprep.ui;

import com.algoprep.config.SettingsStore;
import com.algoprep.display.FontScaleModel;
import com.algoprep.problem.SelectedProblemModel;
import com.algoprep.studied.StudiedStore;
import com.algoprep.theme.NativeThemeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The + and - buttons in the display window. */
class DisplayPanelTest {

    @TempDir
    Path tmp;

    private FontScaleModel scale;
    private DisplayPanel panel;
    private JButton smaller;
    private JButton larger;

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static JButton find(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container inner) {
                JButton found = find(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void build() throws Exception {
        SettingsStore settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        edt(() -> {
            panel = new DisplayPanel(new SelectedProblemModel(), settings, new NativeThemeModel(), scale,
                    new StudiedStore(java.time.Clock.systemDefaultZone(), m -> { }));
            smaller = find(panel, "-");
            larger = find(panel, "+");
        });
    }

    @BeforeEach
    void setUp() {
        scale = new FontScaleModel();
    }

    @Test
    void hasAPlusAndAMinusButtonAboveTheTabs() throws Exception {
        build();

        assertNotNull(smaller);
        assertNotNull(larger);
        assertSame(smaller.getParent(), larger.getParent());
        BorderLayout layout = (BorderLayout) panel.getLayout();
        assertSame(smaller.getParent(), layout.getLayoutComponent(BorderLayout.NORTH));
        assertTrue(layout.getLayoutComponent(BorderLayout.CENTER) instanceof JTabbedPane);
    }

    @Test
    void stillHasTheThreeTabs() throws Exception {
        build();

        JTabbedPane tabs = (JTabbedPane) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertEquals(3, tabs.getTabCount());
        assertEquals("Problem", tabs.getTitleAt(0));
        assertEquals("Notes", tabs.getTitleAt(1));
        assertEquals("My Notes", tabs.getTitleAt(2));
    }

    @Test
    void plusMakesTheTextLargerAndMinusMakesItSmaller() throws Exception {
        build();

        edt(() -> larger.doClick());
        assertEquals(110, scale.percent());
        edt(() -> smaller.doClick());
        edt(() -> smaller.doClick());
        assertEquals(90, scale.percent());
    }

    @Test
    void tooltipsShowTheCurrentSize() throws Exception {
        build();
        edt(() -> larger.doClick());

        assertEquals("Larger text (now 110%)", larger.getToolTipText());
        assertEquals("Smaller text (now 110%)", smaller.getToolTipText());
    }

    @Test
    void plusIsDisabledAtTheLargestSizeAndMinusAtTheSmallest() throws Exception {
        build();

        for (int i = 0; i < 30; i++) {
            edt(() -> larger.doClick());
        }
        assertEquals(FontScaleModel.MAX, scale.percent());
        assertFalse(larger.isEnabled());
        assertTrue(smaller.isEnabled());
        assertTrue(larger.getToolTipText().startsWith("Largest size"), larger.getToolTipText());

        for (int i = 0; i < 40; i++) {
            edt(() -> smaller.doClick());
        }
        assertEquals(FontScaleModel.MIN, scale.percent());
        assertFalse(smaller.isEnabled());
        assertTrue(larger.isEnabled());
        assertTrue(smaller.getToolTipText().startsWith("Smallest size"), smaller.getToolTipText());
    }

    @Test
    void aRestoredSizeIsShownFromTheStart() throws Exception {
        scale.restore(150);

        build();

        assertEquals("Larger text (now 150%)", larger.getToolTipText());
        assertTrue(larger.isEnabled());
        assertTrue(smaller.isEnabled());
    }

    @Test
    void aRestoredLimitStartsWithThatButtonDisabled() throws Exception {
        scale.restore(FontScaleModel.MAX);

        build();

        assertFalse(larger.isEnabled());
        assertTrue(smaller.isEnabled());
    }

    @Test
    void theButtonsDoNotTakeKeyboardFocus() throws Exception {
        build();

        assertFalse(smaller.isFocusable());
        assertFalse(larger.isFocusable());
    }

    @Test
    void aSizeChangeFromOutsideTheButtonsUpdatesThem() throws Exception {
        build();

        scale.restore(FontScaleModel.MIN);
        edt(() -> { }); // the update is applied on the Swing thread

        assertFalse(smaller.isEnabled());
    }
}
