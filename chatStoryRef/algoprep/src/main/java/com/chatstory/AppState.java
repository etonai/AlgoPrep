package com.chatstory;

import com.chatstory.bridge.ErrorCodes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe state machine for the application lifecycle.
 * No JCEF or Swing dependencies — fully unit-testable.
 */
public class AppState {

    public enum State {
        Starting,
        LoadingChatGPT,
        NeedsLogin,
        Ready,
        InjectingPrompt,   // wired in DC003
        Sending,           // wired in DC003
        WaitingForResponse,// wired in DC004
        Complete,          // wired in DC004
        Error
    }

    public interface StateListener {
        void onStateChanged(State previous, State current);
    }

    private State state = State.Starting;
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();

    // -------------------------------------------------------------------------
    // Browser event methods — tolerant of repeated/out-of-order load events
    // -------------------------------------------------------------------------

    public synchronized void browserLoadStarted(String url) {
        if (state == State.LoadingChatGPT) return; // idempotent
        setState(state, State.LoadingChatGPT);
    }

    public synchronized void browserLoadFinished(String url, boolean seemsLoggedIn) {
        if (state != State.LoadingChatGPT) return; // silent no-op
        State next = seemsLoggedIn ? State.Ready : State.NeedsLogin;
        setState(State.LoadingChatGPT, next);
    }

    public synchronized void browserLoadError(String url, String description) {
        System.err.println("[AppState] " + ErrorCodes.PAGE_LOAD_FAILED
                + " url=" + url + " desc=" + description);
        // No state transition in DC002 — full browser error recovery is deferred
    }

    // -------------------------------------------------------------------------
    // Direct transition — used by DC003/DC004 for prompt/response flow
    // -------------------------------------------------------------------------

    public synchronized void transition(State next) {
        if (!isValidDirectTransition(state, next)) {
            throw new IllegalStateException(
                    "Invalid transition: " + state + " → " + next);
        }
        setState(state, next);
    }

    public synchronized void reset() {
        setState(state, State.Ready);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public synchronized State current() {
        return state;
    }

    public synchronized boolean isSendEnabled() {
        return state == State.Ready || state == State.Complete;
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    public void addListener(StateListener l) {
        listeners.add(l);
    }

    public void removeListener(StateListener l) {
        listeners.remove(l);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void setState(State from, State to) {
        state = to;
        // Notify outside the synchronized block is safer (avoids re-entrant lock),
        // but CopyOnWriteArrayList iteration is safe and listeners only call
        // UiThread.run(), so notifying under the lock is acceptable here.
        for (StateListener l : listeners) {
            try {
                l.onStateChanged(from, to);
            } catch (Exception e) {
                System.err.println("[AppState] Listener threw: " + e.getMessage());
            }
        }
    }

    private boolean isValidDirectTransition(State from, State to) {
        switch (from) {
            case Ready:              return to == State.InjectingPrompt;
            case InjectingPrompt:    return to == State.Sending
                    || to == State.Ready
                    || to == State.Error;
            case Sending:            return to == State.WaitingForResponse
                    || to == State.Ready
                    || to == State.Error;
            case WaitingForResponse: return to == State.Complete || to == State.Error;
            case Complete:           return to == State.Ready;
            case Error:              return to == State.Ready;
            default:                 return false;
        }
    }
}
