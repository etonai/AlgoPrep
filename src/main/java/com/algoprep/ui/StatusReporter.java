package com.algoprep.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects status messages before the window exists. Components such as {@code SettingsStore}
 * can report a problem from their constructors; the messages are held until a sink is attached
 * and then flushed in order. Every message is also written to stderr so it is not lost if it is
 * quickly replaced in the status line. No Swing dependency, so it is unit-testable.
 */
public final class StatusReporter {

    private final List<String> pending = new ArrayList<>();
    private Consumer<String> sink;

    public synchronized void report(String message) {
        System.err.println("[Status] " + message);
        if (sink == null) {
            pending.add(message);
        } else {
            sink.accept(message);
        }
    }

    /** Attaches the sink and delivers anything reported earlier. Later calls replace the sink. */
    public synchronized void attach(Consumer<String> newSink) {
        sink = newSink;
        List<String> flush = new ArrayList<>(pending);
        pending.clear();
        flush.forEach(newSink);
    }
}
