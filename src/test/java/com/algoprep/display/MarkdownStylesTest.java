package com.algoprep.display;

import com.algoprep.theme.NativeTheme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownStylesTest {

    private static String joined(NativeTheme theme) {
        return String.join("\n", MarkdownStyles.rules(theme));
    }

    /** The font sizes, in points, in the order they appear in the rules. */
    private static List<Integer> sizes(NativeTheme theme, int percent) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("font-size: (\\d+)pt")
                .matcher(String.join("\n", MarkdownStyles.rules(theme, percent)));
        List<Integer> out = new java.util.ArrayList<>();
        while (m.find()) {
            out.add(Integer.parseInt(m.group(1)));
        }
        return out;
    }

    @Test
    void theNormalSizeIsExactlyTheOriginalStyles() {
        for (NativeTheme theme : NativeTheme.values()) {
            assertEquals(MarkdownStyles.rules(theme), MarkdownStyles.rules(theme, 100));
            // body 12, h1 18, h2 16, h3 14, h4-h6 12, code 11, pre 11
            assertEquals(List.of(12, 18, 16, 14, 12, 11, 11), sizes(theme, 100));
        }
    }

    @Test
    void everyFontSizeScalesWithThePercentage() {
        assertEquals(List.of(24, 36, 32, 28, 24, 22, 22), sizes(NativeTheme.DARK, 200));
        assertEquals(List.of(18, 27, 24, 21, 18, 17, 17), sizes(NativeTheme.LIGHT, 150));
    }

    @Test
    void largerPercentagesNeverShrinkAnySizeAndSmallerNeverGrowAny() {
        for (int p = FontScaleModel.MIN; p < FontScaleModel.MAX; p += FontScaleModel.STEP) {
            List<Integer> now = sizes(NativeTheme.DARK, p);
            List<Integer> next = sizes(NativeTheme.DARK, p + FontScaleModel.STEP);
            for (int i = 0; i < now.size(); i++) {
                assertTrue(next.get(i) >= now.get(i), "size " + i + " at " + p + "%");
            }
        }
        assertTrue(sizes(NativeTheme.DARK, 250).get(0) > sizes(NativeTheme.DARK, 60).get(0));
    }

    @Test
    void sizesNeverDropBelowTheReadableMinimum() {
        for (int p = FontScaleModel.MIN; p <= FontScaleModel.MAX; p += FontScaleModel.STEP) {
            for (int size : sizes(NativeTheme.LIGHT, p)) {
                assertTrue(size >= MarkdownStyles.MIN_POINTS, size + "pt at " + p + "%");
            }
        }
        // Even an extreme value is held at the minimum
        assertTrue(sizes(NativeTheme.DARK, 1).stream().allMatch(s -> s == MarkdownStyles.MIN_POINTS));
    }

    @Test
    void headingsKeepTheirOrderAtEverySize() {
        for (int p = FontScaleModel.MIN; p <= FontScaleModel.MAX; p += FontScaleModel.STEP) {
            List<Integer> s = sizes(NativeTheme.DARK, p); // body, h1, h2, h3, h4-h6, code, pre
            assertTrue(s.get(1) >= s.get(2) && s.get(2) >= s.get(3) && s.get(3) >= s.get(4), p + "%");
        }
    }

    @Test
    void scalingChangesOnlyTheSizesNotTheColors() {
        String normal = String.join("\n", MarkdownStyles.rules(NativeTheme.DARK, 100))
                .replaceAll("font-size: \\d+pt", "font-size: Xpt");
        String big = String.join("\n", MarkdownStyles.rules(NativeTheme.DARK, 200))
                .replaceAll("font-size: \\d+pt", "font-size: Xpt");

        assertEquals(normal, big);
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
