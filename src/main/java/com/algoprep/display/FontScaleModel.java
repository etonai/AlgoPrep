package com.algoprep.display;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The text size of the display window, as a percentage of the normal size. Shared by the three
 * display tabs so switching tabs never changes the size. Pure: no Swing.
 *
 * <p>The size moves in fixed steps within a fixed range, so the + and - buttons can be disabled
 * at the ends instead of silently doing nothing.
 */
public final class FontScaleModel {

    public static final int DEFAULT = 100;
    public static final int STEP = 10;
    public static final int MIN = 60;
    public static final int MAX = 250;

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private int percent = DEFAULT;

    public synchronized int percent()        { return percent; }
    public synchronized boolean canIncrease() { return percent + STEP <= MAX; }
    public synchronized boolean canDecrease() { return percent - STEP >= MIN; }

    public void addListener(Runnable listener) { listeners.add(listener); }

    public void increase() {
        change(percent() + STEP);
    }

    public void decrease() {
        change(percent() - STEP);
    }

    /**
     * Restores a saved value. A missing value, or one that is not on a step within the range (for
     * example from a hand-edited settings file), means the default size.
     */
    public void restore(Integer saved) {
        change(isValid(saved) ? saved : DEFAULT);
    }

    static boolean isValid(Integer value) {
        return value != null && value >= MIN && value <= MAX && (value - DEFAULT) % STEP == 0;
    }

    private void change(int next) {
        synchronized (this) {
            if (next < MIN || next > MAX || next == percent) {
                return;
            }
            percent = next;
        }
        listeners.forEach(Runnable::run);
    }
}
