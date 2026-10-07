package com.algoprep.ui;

import com.algoprep.AppState;
import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ResponseListener;
import com.algoprep.config.SettingsStore;
import com.algoprep.problem.SelectedProblemModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Reset button must work exactly when a send never confirms and the app state is stuck. */
class MainPanelResetTest {

    @TempDir
    Path tmp;

    private final List<String> status = new ArrayList<>();
    private AppState appState;
    private MainPanel panel;
    private JButton send;
    private JButton reset;
    private int resets;

    /** Like the real bridge: reset() forces the app state back to Ready. */
    private final ChatBridge bridge = new ChatBridge() {
        @Override public void sendPrompt(String prompt, ResponseListener l) { }
        @Override public void sendRawPrompt(String prompt, ResponseListener l) { }
        @Override public void reset() { resets++; appState.reset(); }
        @Override public void clickUploadFile() { }
    };

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static JButton find(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container inner) {
                JButton found = find(inner, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @BeforeEach
    void setUp() throws Exception {
        Path instructions = Files.writeString(tmp.resolve("instructions.md"), "Be brief.");
        SettingsStore settings = new SettingsStore(tmp.resolve("settings.json"), m -> { });
        settings.setInstructionsFile(instructions.toString());

        appState = new AppState();
        appState.browserLoadStarted("https://chatgpt.com");
        appState.browserLoadFinished("https://chatgpt.com", true);

        StatusReporter reporter = new StatusReporter();
        reporter.attach(status::add);
        SelectedProblemModel selection = new SelectedProblemModel();

        edt(() -> {
            UploadController uploads = new UploadController(appState, settings, bridge, reporter,
                    selection, tmp.resolve("stage").toString());
            panel = new MainPanel(appState, settings, bridge, reporter, selection, uploads);
            send = find(panel, "Send Instructions");
            reset = find(panel, "Reset");
        });
    }

    @Test
    void theButtonsExistAndResetSitsBesideSend() {
        assertNotNull(send);
        assertNotNull(reset);
        assertSame(send.getParent(), reset.getParent(), "Reset is next to Send Instructions");
    }

    @Test
    void resetRecoversFromAStuckSendAndReenablesSendAtOnce() throws Exception {
        // A send that is never confirmed leaves the state in Sending
        appState.transition(AppState.State.InjectingPrompt);
        appState.transition(AppState.State.Sending);
        edt(() -> { });
        assertFalse(appState.isSendEnabled());
        assertFalse(send.isEnabled(), "stuck: Send Instructions is disabled");
        assertTrue(reset.isEnabled(), "Reset still works");

        edt(() -> reset.doClick());

        assertEquals(1, resets);
        assertEquals(AppState.State.Ready, appState.current());
        assertTrue(send.isEnabled(), "Send Instructions is usable again without waiting for the timer");
    }

    @Test
    void resetMessageIsHonestAboutNotKnowingWhetherTheSendArrived() throws Exception {
        edt(() -> reset.doClick());

        assertEquals(List.of(MainPanel.RESET_MESSAGE), status);
        String text = MainPanel.RESET_MESSAGE.toLowerCase();
        assertTrue(text.contains("check chatgpt"));
        assertFalse(text.contains("was sent"));
        assertFalse(text.contains("attached"));
    }

    @Test
    void resetStaysEnabledWhileEverythingElseIsDisabled() throws Exception {
        appState.browserLoadStarted("https://chatgpt.com"); // page loading: not Ready
        edt(() -> { });

        assertFalse(send.isEnabled());
        assertTrue(reset.isEnabled(), "Reset does not depend on the app state");

        appState.reset();
        appState.transition(AppState.State.InjectingPrompt);
        edt(() -> { });

        assertFalse(send.isEnabled());
        assertTrue(reset.isEnabled());
    }
}
