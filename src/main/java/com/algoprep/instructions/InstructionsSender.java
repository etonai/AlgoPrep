package com.algoprep.instructions;

import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ErrorCodes;
import com.algoprep.bridge.ResponseListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Reads the instructions file and sends it to ChatGPT as a plain message with no response
 * tracking (Plan sections 6.1 and 9). No Swing dependency, so it is unit-testable with a fake
 * {@link ChatBridge}.
 *
 * <p>The file is read on every send so external edits take effect. After an uncertain failure
 * nothing is retried: a retry could duplicate the message.
 */
public final class InstructionsSender {

    /** Result of reading the file: exactly one of {@code text} and {@code problem} is non-null. */
    public record Loaded(String text, String problem) {
        public boolean ok() { return problem == null; }
    }

    private InstructionsSender() {}

    /** Reads the file as UTF-8. Reports a missing, unreadable or blank file as a problem. */
    public static Loaded load(String path) {
        if (path == null || path.isBlank()) {
            return new Loaded(null, "Choose an instructions file.");
        }
        Path file;
        try {
            file = Path.of(path);
        } catch (RuntimeException e) {
            return new Loaded(null, "Instructions file path is not valid.");
        }
        if (!Files.isRegularFile(file)) {
            return new Loaded(null, "Instructions file not found: " + file.getFileName());
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) {
                return new Loaded(null, "Instructions file is empty: " + file.getFileName());
            }
            return new Loaded(text, null);
        } catch (IOException e) {
            return new Loaded(null, "Instructions file could not be read: " + e.getMessage());
        }
    }

    /**
     * Sends the file's text. Reports the outcome through {@code status}; calls {@code onSent}
     * only when the bridge confirms the message appeared in ChatGPT. Callbacks may arrive on
     * any thread.
     *
     * @param browserReady whether the browser can send now ({@code AppState.isSendEnabled()}).
     *                     Checked before the bridge is reset, because a reset forces the state to
     *                     Ready from anywhere and would hide a page that is still loading.
     */
    public static void send(String path, BooleanSupplier browserReady, ChatBridge bridge,
                            Consumer<String> status, Runnable onSent) {
        Loaded loaded = load(path);
        if (!loaded.ok()) {
            status.accept(loaded.problem());
            return;
        }
        if (!browserReady.getAsBoolean()) {
            status.accept(describeError(ErrorCodes.SEND_BUTTON_DISABLED, null));
            return;
        }
        // Reset-before-send mirrors ChatStory's DC23 workaround for stuck states (Plan section 6.1)
        bridge.reset();
        bridge.sendRawPrompt(loaded.text(), new ResponseListener() {
            @Override
            public void onPromptSubmitted(long requestId) {
                status.accept("Instructions message sent");
                onSent.run();
            }

            @Override
            public void onResponsePartial(long requestId, String responseText) { }

            @Override
            public void onResponseComplete(long requestId, String responseText, String responseHtml) { }

            @Override
            public void onError(long requestId, String errorCode, String message) {
                status.accept(describeError(errorCode, message));
            }
        });
    }

    /** Turns a bridge error into wording that says what was and was not observed. */
    public static String describeError(String code, String message) {
        String detail = (message == null || message.isBlank()) ? "" : " (" + message + ")";
        if (code == null) {
            return "Send failed" + detail + ". Check ChatGPT before pressing again.";
        }
        return switch (code) {
            case ErrorCodes.SEND_BUTTON_DISABLED ->
                    "ChatGPT is not ready to send. Wait for it to finish loading or log in, then try again.";
            case ErrorCodes.LOGIN_REQUIRED ->
                    "Please log in to ChatGPT, then try again.";
            case ErrorCodes.EDITOR_NOT_FOUND, ErrorCodes.PROMPT_INJECTION_FAILED ->
                    "Could not put the instructions into ChatGPT's input box" + detail
                            + ". Nothing was sent.";
            case ErrorCodes.SEND_BUTTON_NOT_FOUND, ErrorCodes.SEND_CLICK_FAILED ->
                    "Could not press Send in ChatGPT" + detail
                            + ". Check ChatGPT; the text may still be in the input box.";
            case ErrorCodes.USER_MESSAGE_NOT_CONFIRMED, ErrorCodes.TIMEOUT ->
                    "Send was not confirmed" + detail
                            + ". Check ChatGPT; the message may or may not have been sent. "
                            + "Press the button again only if it did not arrive.";
            default ->
                    "Send failed: " + code + detail + ". Check ChatGPT before pressing again.";
        };
    }
}
