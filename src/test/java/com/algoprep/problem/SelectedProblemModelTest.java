package com.algoprep.problem;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SelectedProblemModelTest {

    private static Problem p(String key, int number, String title) {
        return new Problem(key, number, title, Path.of(key + "_problem.md"), Optional.empty());
    }

    private final Problem twoSum = p("0001_two-sum", 1, "Two Sum");
    private final Problem addTwo = p("0002_add-two", 2, "Add Two");

    @Test
    void startsEmpty() {
        SelectedProblemModel m = new SelectedProblemModel();
        assertTrue(m.current().isEmpty());
        assertTrue(m.selectedKey().isEmpty());
        assertFalse(m.isUnavailable());
        assertEquals("Selected: (none)", m.describe());
    }

    @Test
    void selectAndClear() {
        SelectedProblemModel m = new SelectedProblemModel();
        m.select(twoSum);
        assertEquals(twoSum, m.current().orElseThrow());
        assertEquals("0001_two-sum", m.selectedKey().orElseThrow());
        assertEquals("Selected: 1 - Two Sum", m.describe());

        m.clear();
        assertTrue(m.current().isEmpty());
        assertTrue(m.selectedKey().isEmpty());
    }

    @Test
    void refreshKeepsTheSelectionByKeyEvenWhenOrderChanges() {
        SelectedProblemModel m = new SelectedProblemModel();
        m.select(twoSum);
        Problem reloaded = p("0001_two-sum", 1, "Two Sum");

        m.reconcile(List.of(addTwo, reloaded));

        assertEquals(reloaded, m.current().orElseThrow());
        assertFalse(m.isUnavailable());
    }

    @Test
    void refreshWithStatementGoneMarksUnavailableAndNeverSelectsAnotherProblem() {
        SelectedProblemModel m = new SelectedProblemModel();
        m.select(twoSum);

        m.reconcile(List.of(addTwo));

        assertTrue(m.isUnavailable());
        assertTrue(m.current().isEmpty(), "unavailable means no usable current problem");
        assertEquals("0001_two-sum", m.selectedKey().orElseThrow());
        assertEquals("Selected: 1 - Two Sum (unavailable)", m.describe());
    }

    @Test
    void unavailableProblemBecomesAvailableWhenItReturns() {
        SelectedProblemModel m = new SelectedProblemModel();
        m.select(twoSum);
        m.reconcile(List.of());
        assertTrue(m.isUnavailable());

        m.reconcile(List.of(twoSum));

        assertFalse(m.isUnavailable());
        assertEquals(twoSum, m.current().orElseThrow());
    }

    @Test
    void reconcileWithNothingSelectedDoesNothing() {
        SelectedProblemModel m = new SelectedProblemModel();
        AtomicInteger fired = new AtomicInteger();
        m.addListener(fired::incrementAndGet);

        m.reconcile(List.of(twoSum));

        assertTrue(m.current().isEmpty());
        assertEquals(0, fired.get());
    }

    @Test
    void listenerFiresOnRealChangesOnly() {
        SelectedProblemModel m = new SelectedProblemModel();
        AtomicInteger fired = new AtomicInteger();
        m.addListener(fired::incrementAndGet);

        m.select(twoSum);
        assertEquals(1, fired.get());
        m.select(twoSum);
        assertEquals(1, fired.get());
        m.reconcile(List.of(twoSum));
        assertEquals(1, fired.get());
        m.reconcile(List.of());
        assertEquals(2, fired.get());
        m.clear();
        assertEquals(3, fired.get());
        m.clear();
        assertEquals(3, fired.get());
    }

    @Test
    void restoreSelectsOnlyWhenTheKeyIsFound() {
        SelectedProblemModel m = new SelectedProblemModel();

        assertFalse(m.restore("0009_missing", List.of(twoSum)));
        assertTrue(m.current().isEmpty());
        assertFalse(m.restore(null, List.of(twoSum)));

        assertTrue(m.restore("0001_two-sum", List.of(twoSum, addTwo)));
        assertEquals(twoSum, m.current().orElseThrow());
    }

    @Test
    void keysMatchCaseInsensitively() {
        SelectedProblemModel m = new SelectedProblemModel();
        assertTrue(m.restore("0001_TWO-SUM", List.of(twoSum)));
        assertEquals("0001_two-sum", m.selectedKey().orElseThrow());
    }
}
