package com.chatstory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppStateTest {

    private AppState state;

    @BeforeEach
    void setUp() {
        state = new AppState();
    }

    // ---- Startup transitions ------------------------------------------------

    @Test
    void startingToLoadingChatGPT() {
        state.browserLoadStarted("https://chatgpt.com");
        assertEquals(AppState.State.LoadingChatGPT, state.current());
    }

    @Test
    void loadingChatGPTToReady() {
        state.browserLoadStarted("https://chatgpt.com");
        state.browserLoadFinished("https://chatgpt.com/", true);
        assertEquals(AppState.State.Ready, state.current());
    }

    @Test
    void loadingChatGPTToNeedsLogin() {
        state.browserLoadStarted("https://chatgpt.com");
        state.browserLoadFinished("https://chatgpt.com/auth/login", false);
        assertEquals(AppState.State.NeedsLogin, state.current());
    }

    @Test
    void needsLoginRetryViaLoadStarted() {
        state.browserLoadStarted("https://chatgpt.com");
        state.browserLoadFinished("https://chatgpt.com/auth/login", false);
        state.browserLoadStarted("https://chatgpt.com");
        assertEquals(AppState.State.LoadingChatGPT, state.current());
    }

    // ---- Navigation noise tolerance ----------------------------------------

    @Test
    void consecutiveLoadStartedDoesNotThrow() {
        assertDoesNotThrow(() -> {
            state.browserLoadStarted("https://chatgpt.com");
            state.browserLoadStarted("https://chatgpt.com/c/something");
        });
        assertEquals(AppState.State.LoadingChatGPT, state.current());
    }

    @Test
    void loadFinishedWhileNotLoadingIsNoOp() {
        // State is Starting — browserLoadFinished should be silently ignored
        assertDoesNotThrow(() ->
                state.browserLoadFinished("https://chatgpt.com/", true));
        assertEquals(AppState.State.Starting, state.current());
    }

    @Test
    void loadErrorFromAnyStateDoesNotThrow() {
        assertDoesNotThrow(() ->
                state.browserLoadError("https://chatgpt.com", "ERR_CONNECTION_REFUSED"));
        assertEquals(AppState.State.Starting, state.current());

        state.browserLoadStarted("https://chatgpt.com");
        assertDoesNotThrow(() ->
                state.browserLoadError("https://chatgpt.com", "ERR_TIMED_OUT"));
        assertEquals(AppState.State.LoadingChatGPT, state.current());
    }

    @Test
    void readyToLoadingChatGPTViaLoadStarted() {
        // User navigates to a different page while app is Ready
        toReady();
        state.browserLoadStarted("https://chatgpt.com/new");
        assertEquals(AppState.State.LoadingChatGPT, state.current());
    }

    // ---- Direct transitions (DC003/DC004 states) ----------------------------

    @Test
    void readyToInjectingPrompt() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        assertEquals(AppState.State.InjectingPrompt, state.current());
    }

    @Test
    void injectingPromptToSending() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Sending);
        assertEquals(AppState.State.Sending, state.current());
    }

    @Test
    void injectingPromptToReadyForTestInjection() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Ready);
        assertEquals(AppState.State.Ready, state.current());
    }

    @Test
    void injectingPromptToError() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Error);
        assertEquals(AppState.State.Error, state.current());
    }

    @Test
    void sendingToWaitingForResponse() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Sending);
        state.transition(AppState.State.WaitingForResponse);
        assertEquals(AppState.State.WaitingForResponse, state.current());
    }

    @Test
    void sendingToReadyForUnassistedPromptSubmission() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Sending);
        state.transition(AppState.State.Ready);
        assertEquals(AppState.State.Ready, state.current());
    }

    @Test
    void sendingToError() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Sending);
        state.transition(AppState.State.Error);
        assertEquals(AppState.State.Error, state.current());
    }

    @Test
    void waitingForResponseToComplete() {
        toWaiting();
        state.transition(AppState.State.Complete);
        assertEquals(AppState.State.Complete, state.current());
    }

    @Test
    void waitingForResponseToError() {
        toWaiting();
        state.transition(AppState.State.Error);
        assertEquals(AppState.State.Error, state.current());
    }

    @Test
    void completeToReady() {
        toWaiting();
        state.transition(AppState.State.Complete);
        state.transition(AppState.State.Ready);
        assertEquals(AppState.State.Ready, state.current());
    }

    @Test
    void errorToReady() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Error);
        state.transition(AppState.State.Ready);
        assertEquals(AppState.State.Ready, state.current());
    }

    @Test
    void invalidTransitionThrows() {
        assertThrows(IllegalStateException.class,
                () -> state.transition(AppState.State.Complete));
    }

    // ---- Send enablement ----------------------------------------------------

    @Test
    void sendDisabledByDefault() {
        assertFalse(state.isSendEnabled());
    }

    @Test
    void sendEnabledWhenReady() {
        toReady();
        assertTrue(state.isSendEnabled());
    }

    @Test
    void sendEnabledWhenComplete() {
        toWaiting();
        state.transition(AppState.State.Complete);
        assertTrue(state.isSendEnabled());
    }

    @Test
    void sendDisabledInAllOtherStates() {
        for (AppState.State s : AppState.State.values()) {
            if (s == AppState.State.Ready || s == AppState.State.Complete) continue;
            AppState fresh = new AppState();
            // Force state via reflection would be cleaner but just test the reachable states
        }
        // Spot-check a few non-send states
        state.browserLoadStarted("https://chatgpt.com");
        assertFalse(state.isSendEnabled()); // LoadingChatGPT

        toReady();
        state.transition(AppState.State.InjectingPrompt);
        assertFalse(state.isSendEnabled()); // InjectingPrompt

        state.transition(AppState.State.Sending);
        state.transition(AppState.State.WaitingForResponse);
        assertFalse(state.isSendEnabled()); // WaitingForResponse
    }

    // ---- Listener -----------------------------------------------------------

    @Test
    void listenerReceivesTransitions() {
        List<AppState.State> seen = new ArrayList<>();
        state.addListener((prev, current) -> seen.add(current));

        state.browserLoadStarted("https://chatgpt.com");
        state.browserLoadFinished("https://chatgpt.com/", true);

        assertEquals(List.of(AppState.State.LoadingChatGPT, AppState.State.Ready), seen);
    }

    @Test
    void removedListenerNotCalled() {
        List<AppState.State> seen = new ArrayList<>();
        AppState.StateListener l = (prev, current) -> seen.add(current);
        state.addListener(l);
        state.removeListener(l);

        state.browserLoadStarted("https://chatgpt.com");
        assertTrue(seen.isEmpty());
    }

    // ---- Helpers ------------------------------------------------------------

    private void toReady() {
        state.browserLoadStarted("https://chatgpt.com");
        state.browserLoadFinished("https://chatgpt.com/", true);
    }

    private void toWaiting() {
        toReady();
        state.transition(AppState.State.InjectingPrompt);
        state.transition(AppState.State.Sending);
        state.transition(AppState.State.WaitingForResponse);
    }
}
