package com.algoprep.display;

import com.algoprep.theme.NativeTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownStylesTest {

    private static String joined(NativeTheme theme) {
        return String.join("\n", MarkdownStyles.rules(theme));
    }

    @Test
    void darkAndLightRulesDiffer() {
        assertNotEquals(joined(NativeTheme.DARK), joined(NativeTheme.LIGHT));
        assertNotEquals(MarkdownStyles.background(NativeTheme.DARK), MarkdownStyles.background(NativeTheme.LIGHT));
    }

    @Test
    void bothThemesStyleBodyCodeAndPreWithMonospacedBlocks() {
        for (NativeTheme theme : NativeTheme.values()) {
            List<String> rules = MarkdownStyles.rules(theme);
            assertTrue(rules.stream().anyMatch(r -> r.startsWith("body")), theme + " body");
            assertTrue(rules.stream().anyMatch(r -> r.startsWith("pre") && r.contains("monospaced")), theme + " pre");
            assertTrue(rules.stream().anyMatch(r -> r.startsWith("code") && r.contains("monospaced")), theme + " code");
            assertTrue(rules.stream().anyMatch(r -> r.startsWith("a ")), theme + " link");
            assertTrue(rules.stream().anyMatch(r -> r.startsWith("th")), theme + " table header");
        }
    }

    @Test
    void bodyBackgroundMatchesTheThemeBackground() {
        for (NativeTheme theme : NativeTheme.values()) {
            assertTrue(joined(theme).contains("background-color: " + MarkdownStyles.background(theme)));
        }
    }

    @Test
    void darkMatchesNativeThemeApplierFieldAndTextColors() {
        // NativeThemeApplier: DARK_FIELD (28,30,34) = #1c1e22, DARK_TEXT (232,234,237) = #e8eaed
        assertEquals("#1c1e22", MarkdownStyles.background(NativeTheme.DARK));
        assertTrue(joined(NativeTheme.DARK).contains("color: #e8eaed"));
        // LIGHT_FIELD is white and LIGHT_TEXT is (30,30,30) = #1e1e1e
        assertEquals("#ffffff", MarkdownStyles.background(NativeTheme.LIGHT));
        assertTrue(joined(NativeTheme.LIGHT).contains("color: #1e1e1e"));
    }

    @Test
    void codeBlocksAreVisiblyDifferentFromThePageBackground() {
        for (NativeTheme theme : NativeTheme.values()) {
            String pre = MarkdownStyles.rules(theme).stream().filter(r -> r.startsWith("pre")).findFirst().orElseThrow();
            assertFalse(pre.contains("background-color: " + MarkdownStyles.background(theme) + ";"), theme.toString());
        }
    }
}
