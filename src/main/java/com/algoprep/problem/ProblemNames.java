package com.algoprep.problem;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses base keys and formats display names (Plan section 5.2). No Swing dependency. */
public final class ProblemNames {

    /** A base key split into its number and the rest. */
    public record KeyParts(int number, String slug) { }

    private static final Pattern KEY = Pattern.compile("^(\\d+)_(.+)$");

    private ProblemNames() {}

    /**
     * Splits {@code 0001_two-sum} into number 1 and slug {@code two-sum}. Empty when the key does
     * not start with digits and an underscore, has no slug, or has a number too large for an int.
     */
    public static Optional<KeyParts> parseKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        Matcher m = KEY.matcher(key);
        if (!m.matches() || m.group(2).isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new KeyParts(Integer.parseInt(m.group(1)), m.group(2)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** {@code two-sum} becomes {@code Two Sum}: hyphens and underscores to spaces, title-cased. */
    public static String titleOf(String slug) {
        String[] words = slug.replace('-', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.substring(0, 1).toUpperCase(Locale.ROOT))
              .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    /** {@code 1} and {@code Two Sum} become {@code 1 - Two Sum}. */
    public static String display(int number, String title) {
        return number + " - " + title;
    }

    /**
     * Display name for a key, for example {@code 0001_two-sum} becomes {@code 1 - Two Sum}.
     * A key that does not parse is returned unchanged.
     */
    public static String displayForKey(String key) {
        return parseKey(key)
                .map(p -> display(p.number(), titleOf(p.slug())))
                .orElse(key);
    }
}
