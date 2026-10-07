package com.algoprep.problem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

/**
 * The selected problem. The Problems tab sets it, and the MAIN tab (and later the display pane and
 * Upload) observe it.
 *
 * <p>Selection is by base key. If a refresh no longer finds the statement, the selection stays on
 * that key and is marked <em>unavailable</em>; it never silently moves to another problem
 * (Plan section 5.3).
 */
public final class SelectedProblemModel {

    private record State(String key, Problem problem, boolean unavailable) { }

    private static final State EMPTY = new State(null, null, false);

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private State state = EMPTY;

    public void addListener(Runnable listener) { listeners.add(listener); }

    /** The selected problem, present only while its statement is still found. */
    public synchronized Optional<Problem> current() {
        return state.unavailable() ? Optional.empty() : Optional.ofNullable(state.problem());
    }

    public synchronized Optional<String> selectedKey() { return Optional.ofNullable(state.key()); }

    public synchronized boolean isUnavailable() { return state.unavailable(); }

    public void select(Problem problem) {
        change(s -> new State(problem.key(), problem, false));
    }

    public void clear() {
        change(s -> EMPTY);
    }

    /** Re-finds the selected key in a fresh list. Missing means unavailable, not a new selection. */
    public void reconcile(List<Problem> problems) {
        change(s -> {
            if (s.key() == null) {
                return s;
            }
            return find(problems, s.key())
                    .map(p -> new State(p.key(), p, false))
                    .orElseGet(() -> new State(s.key(), s.problem(), true));
        });
    }

    /** Selects the saved key only if the list contains it. Returns whether it was found. */
    public boolean restore(String savedKey, List<Problem> problems) {
        if (savedKey == null) {
            return false;
        }
        Optional<Problem> found = find(problems, savedKey);
        found.ifPresent(this::select);
        return found.isPresent();
    }

    /** Text for the MAIN tab: {@code Selected: 1 - Two Sum}, {@code (unavailable)} or {@code (none)}. */
    public synchronized String describe() {
        if (state.key() == null) {
            return "Selected: (none)";
        }
        String name = state.problem() != null
                ? state.problem().displayName()
                : ProblemNames.displayForKey(state.key());
        return "Selected: " + name + (state.unavailable() ? " (unavailable)" : "");
    }

    private static Optional<Problem> find(List<Problem> problems, String key) {
        return problems.stream().filter(p -> p.key().equalsIgnoreCase(key)).findFirst();
    }

    private void change(UnaryOperator<State> update) {
        synchronized (this) {
            State next = update.apply(state);
            if (next.equals(state)) {
                return;
            }
            state = next;
        }
        listeners.forEach(Runnable::run);
    }
}
