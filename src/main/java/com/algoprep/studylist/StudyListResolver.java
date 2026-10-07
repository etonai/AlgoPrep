package com.algoprep.studylist;

import com.algoprep.problem.Problem;
import com.algoprep.problem.ProblemNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Matches study list entries to the problems found in the PROBLEMS directory. Pure: no Swing.
 *
 * <p>The match is by number <em>and</em> slug, not by the exact text of the key, so
 * {@code 99_nodifficulty} in the list finds {@code 0099_nodifficulty} on disk. The number alone never
 * matches and neither does a partial slug, so {@code 0001_two-sum-ii} is never matched to
 * {@code 0001_two-sum} (Plan section 5.1). A key that does not look like {@code number_slug} cannot
 * match. The result keeps the list's order.
 */
public final class StudyListResolver {

    private record Identity(int number, String slug) { }

    private StudyListResolver() {}

    public static List<StudyListRow> resolve(List<StudyListEntry> entries, List<Problem> problems) {
        Map<Identity, Problem> byIdentity = new HashMap<>();
        for (Problem problem : problems) {
            identityOf(problem.key()).ifPresent(id -> byIdentity.putIfAbsent(id, problem));
        }
        List<StudyListRow> rows = new ArrayList<>();
        for (StudyListEntry entry : entries) {
            Optional<Problem> match = identityOf(entry.key()).map(byIdentity::get);
            rows.add(new StudyListRow(entry, match));
        }
        return rows;
    }

    private static Optional<Identity> identityOf(String key) {
        return ProblemNames.parseKey(key)
                .map(parts -> new Identity(parts.number(), parts.slug().toLowerCase(Locale.ROOT)));
    }
}
