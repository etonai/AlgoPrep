package com.chatstory.bridge;

import com.chatstory.AppState;
import com.chatstory.browser.DomBridge;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ChatGptBridge implements ChatBridge {

    public static final int USER_MESSAGE_CONFIRM_TIMEOUT_MS = 5000;
    public static final int SEND_OPERATION_TIMEOUT_MS = 10000;
    public static final int RESPONSE_POLL_INTERVAL_MS = 500;
    public static final int RESPONSE_STABILITY_WINDOW_MS = 1500;
    public static final int RESPONSE_TIMEOUT_MS = 180000;

    private static final Gson GSON = new Gson();

    private final DomBridge domBridge;
    private final CefBrowser browser;
    private final AppState appState;
    private final RequestIdGenerator requestIds = new RequestIdGenerator();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chatgpt-bridge-timeouts");
        t.setDaemon(true);
        return t;
    });

    private final Object lock = new Object();

    private long activeRequestId = 0L;
    private ResponseListener activeListener;
    private String activePrompt;
    private ScheduledFuture<?> activeTimeout;
    private boolean injectOnly;
    private boolean trackResponse;
    private String injectScript;
    private String sendScript;
    private String extractScript;
    private String fetchScript;
    private volatile BiConsumer<String, String> manualFetchCallback;
    private JsonObject selectors;

    public ChatGptBridge(DomBridge domBridge, CefBrowser browser, AppState appState) {
        this.domBridge = domBridge;
        this.browser = browser;
        this.appState = appState;

        domBridge.registerHandler("injectResult", this::handleInjectResult);
        domBridge.registerHandler("sendResult", this::handleSendResult);
        domBridge.registerHandler("responseComplete", this::handleResponseComplete);
        domBridge.registerHandler("error", this::handleErrorResult);
        domBridge.registerHandler("manualFetch", this::handleManualFetch);
    }

    @Override
    public void sendPrompt(String prompt, ResponseListener listener) {
        startPrompt(prompt, listener, false, true);
    }

    @Override
    public void sendRawPrompt(String prompt, ResponseListener listener) {
        startPrompt(prompt, listener, false, false);
    }

    public void testInjectPrompt(String prompt, ResponseListener listener) {
        startPrompt(prompt, listener, true, false);
    }

    @Override
    public void reset() {
        synchronized (lock) {
            clearActiveLocked();
        }
        appState.reset();
    }

    @Override
    public void clickUploadFile() {
        browser.setFocus(true);
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(200); // let focus settle before sending keystrokes
                Robot robot = new Robot();
                // release any modifier keys the user may still be holding
                robot.keyRelease(KeyEvent.VK_SHIFT);
                robot.keyRelease(KeyEvent.VK_CONTROL);
                robot.keyRelease(KeyEvent.VK_ALT);
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_U);
                robot.keyRelease(KeyEvent.VK_U);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            } catch (AWTException | InterruptedException e) {
                System.err.println("[ChatGptBridge] clickUploadFile failed: " + e.getMessage());
            }
        });
    }

    public void fetchLatestResponse(BiConsumer<String, String> onResult) {
        log("fetchLatestResponse requested thread=" + Thread.currentThread().getName());
        manualFetchCallback = onResult;
        JsonObject options = new JsonObject();
        options.addProperty("requestId", 0L);
        executeFunction("/js/fetch_response.js", "window.chatStoryFetchResponse", options);
    }

    private void handleManualFetch(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        log("handleManualFetch enter requestId=" + message.getRequestId() + " ok=" + message.isOk());
        BiConsumer<String, String> cb = manualFetchCallback;
        manualFetchCallback = null;
        callback.success("");
        if (cb != null) {
            String text = message.isOk() && message.getText() != null ? message.getText() : "";
            String html = message.isOk() && message.getHtml() != null ? message.getHtml() : "";
            log("handleManualFetch invoking callback requestId=" + message.getRequestId()
                    + " thread=" + Thread.currentThread().getName()
                    + " textLen=" + text.length());
            cb.accept(text, html);
            log("handleManualFetch callback returned requestId=" + message.getRequestId());
        } else {
            log("handleManualFetch no callback registered requestId=" + message.getRequestId());
        }
    }

    private void startPrompt(String prompt, ResponseListener listener, boolean injectOnly,
                             boolean trackResponse) {
        String text = prompt == null ? "" : prompt;
        long requestId = requestIds.next();
        log("startPrompt requestId=" + requestId + " injectOnly=" + injectOnly + " trackResponse=" + trackResponse);

        synchronized (lock) {
            if (!appState.isSendEnabled() || activeRequestId != 0L) {
                listener.onError(requestId, ErrorCodes.SEND_BUTTON_DISABLED,
                        "Application is not ready to send");
                return;
            }
            activeRequestId = requestId;
            activeListener = listener;
            activePrompt = text;
            this.injectOnly = injectOnly;
            this.trackResponse = trackResponse;
            activeTimeout = scheduler.schedule(
                    () -> timeoutRequest(requestId),
                    SEND_OPERATION_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS);
        }

        try {
            if (appState.current() == AppState.State.Complete) {
                transition(AppState.State.Ready);
            }
            transition(AppState.State.InjectingPrompt);
            JsonObject options = new JsonObject();
            options.addProperty("requestId", requestId);
            options.add("selectors", selectorSubset("promptEditor", "sendButton"));
            options.addProperty("text", text);
            executeFunction("/js/inject_prompt.js", "window.chatStoryInjectPrompt", options);
        } catch (Exception e) {
            failActive(requestId, ErrorCodes.PROMPT_INJECTION_FAILED, e.getMessage());
        }
    }

    private void handleInjectResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        log("handleInjectResult enter requestId=" + message.getRequestId() + " ok=" + message.isOk());
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        if (!message.isOk()) {
            failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.PROMPT_INJECTION_FAILED),
                    fallback(message.getMessage(), "Prompt injection failed"));
            callback.success("");
            return;
        }

        if (isInjectOnly()) {
            ResponseListener listener;
            synchronized (lock) {
                listener = activeListener;
                clearActiveLocked();
            }
            transition(AppState.State.Ready);
            if (listener != null) {
                listener.onPromptSubmitted(message.getRequestId());
            }
            callback.success("");
            return;
        }

        try {
            transition(AppState.State.Sending);
            JsonObject options = new JsonObject();
            options.addProperty("requestId", message.getRequestId());
            options.add("selectors", selectorSubset("sendButton", "userMsg", "assistantMsg"));
            options.addProperty("expectedText", activePrompt);
            options.addProperty("timeoutMs", USER_MESSAGE_CONFIRM_TIMEOUT_MS);
            executeFunction("/js/trigger_send.js", "window.chatStoryTriggerSend", options);
        } catch (Exception e) {
            failActive(message.getRequestId(), ErrorCodes.SEND_CLICK_FAILED, e.getMessage());
        }
        callback.success("");
    }

    private void handleSendResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        log("handleSendResult enter requestId=" + message.getRequestId() + " ok=" + message.isOk());
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        if (!message.isOk()) {
            failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.USER_MESSAGE_NOT_CONFIRMED),
                    fallback(message.getMessage(), "Prompt submission was not confirmed"));
            callback.success("");
            return;
        }

        ResponseListener listener;
        synchronized (lock) {
            listener = activeListener;
            cancelActiveTimeoutLocked();
        }
        if (listener != null) {
            listener.onPromptSubmitted(message.getRequestId());
        }
        if (!isTrackingResponse()) {
            synchronized (lock) {
                clearActiveLocked();
            }
            transition(AppState.State.Ready);
            callback.success("");
            return;
        }
        try {
            transition(AppState.State.WaitingForResponse);
            JsonObject options = new JsonObject();
            options.addProperty("requestId", message.getRequestId());
            options.add("selectors", selectorSubset("assistantMsg", "stopButton", "sendButton"));
            options.addProperty("assistantBeforeCount", parseLong(message.getText(), 0L));
            options.addProperty("pollIntervalMs", RESPONSE_POLL_INTERVAL_MS);
            options.addProperty("stabilityWindowMs", RESPONSE_STABILITY_WINDOW_MS);
            options.addProperty("timeoutMs", RESPONSE_TIMEOUT_MS);
            executeFunction("/js/extract_response.js", "window.chatStoryExtractResponse", options);
        } catch (Exception e) {
            failActive(message.getRequestId(), ErrorCodes.EXTRACTION_FAILED, e.getMessage());
        }
        callback.success("");
    }

    private void handleResponseComplete(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        log("handleResponseComplete enter requestId=" + message.getRequestId()
                + " ok=" + message.isOk()
                + " thread=" + Thread.currentThread().getName());
        if (!isActive(message)) {
            log("handleResponseComplete callback.success before stale requestId=" + message.getRequestId());
            callback.success("");
            log("handleResponseComplete callback.success after stale requestId=" + message.getRequestId());
            return;
        }

        ResponseListener listener;
        synchronized (lock) {
            listener = activeListener;
        }

        if (!message.isOk()) {
            if (listener != null && message.getText() != null && !message.getText().isBlank()) {
                listener.onResponsePartial(message.getRequestId(), message.getText());
            }
            failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.EXTRACTION_FAILED),
                    fallback(message.getMessage(), "Response extraction failed"));
            log("handleResponseComplete callback.success before failure requestId=" + message.getRequestId());
            callback.success("");
            log("handleResponseComplete callback.success after failure requestId=" + message.getRequestId());
            return;
        }

        synchronized (lock) {
            clearActiveLocked();
        }
        transition(AppState.State.Complete);
        if (listener != null) {
            log("handleResponseComplete invoking listener.onResponseComplete requestId=" + message.getRequestId()
                    + " thread=" + Thread.currentThread().getName()
                    + " textLen=" + (message.getText() == null ? 0 : message.getText().length()));
            listener.onResponseComplete(message.getRequestId(), message.getText(), message.getHtml());
            log("handleResponseComplete listener.onResponseComplete returned requestId=" + message.getRequestId());
        }
        log("handleResponseComplete callback.success before requestId=" + message.getRequestId());
        callback.success("");
        log("handleResponseComplete callback.success after requestId=" + message.getRequestId());
    }

    private void handleErrorResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        log("handleErrorResult enter requestId=" + message.getRequestId());
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.BRIDGE_HANDLER_FAILED),
                fallback(message.getMessage(), "Bridge reported an error"));
        callback.success("");
    }

    private void transition(AppState.State next) {
        log("appState.transition -> " + next);
        appState.transition(next);
        log("appState.transition -> " + next + " done");
    }

    private void log(String message) {
        System.out.println("[ChatGptBridge] " + message);
    }

    private boolean isActive(BridgeMessage message) {
        synchronized (lock) {
            if (message.getRequestId() == activeRequestId) {
                return true;
            }
            System.out.println("[ChatGptBridge] Dropping stale " + message.getType()
                    + " requestId=" + message.getRequestId()
                    + " activeRequestId=" + activeRequestId);
            return false;
        }
    }

    private void timeoutRequest(long requestId) {
        failActive(requestId, ErrorCodes.TIMEOUT, "Send operation timed out");
    }

    private void failActive(long requestId, String errorCode, String message) {
        ResponseListener listener;
        synchronized (lock) {
            if (requestId != activeRequestId) {
                System.out.println("[ChatGptBridge] Ignoring stale failure requestId="
                        + requestId + " activeRequestId=" + activeRequestId);
                return;
            }
            listener = activeListener;
            clearActiveLocked();
        }

        log("failActive requestId=" + requestId + " errorCode=" + errorCode + " message=" + message);
        try {
            transition(AppState.State.Error);
        } catch (IllegalStateException ignored) {
            // If browser navigation changed state underneath us, still report the error.
        }
        if (listener != null) {
            listener.onError(requestId, errorCode, message);
        }
    }

    private void clearActiveLocked() {
        cancelActiveTimeoutLocked();
        activeRequestId = 0L;
        activeListener = null;
        activePrompt = null;
        injectOnly = false;
        trackResponse = false;
    }

    private void cancelActiveTimeoutLocked() {
        if (activeTimeout != null) {
            activeTimeout.cancel(false);
        }
        activeTimeout = null;
    }

    private boolean isInjectOnly() {
        synchronized (lock) {
            return injectOnly;
        }
    }

    private boolean isTrackingResponse() {
        synchronized (lock) {
            return trackResponse;
        }
    }

    private void executeFunction(String resourcePath, String functionName, JsonObject options) {
        log("executeFunction dispatching " + functionName + " (" + resourcePath + ")");
        String script = loadScript(resourcePath) + "\n" + functionName + "(" + GSON.toJson(options) + ");";
        domBridge.execute(browser, script);
    }

    private JsonObject selectorSubset(String... keys) {
        JsonObject source = selectors();
        JsonObject subset = new JsonObject();
        for (String key : keys) {
            if (source.has(key)) {
                subset.add(key, source.get(key));
            }
        }
        return subset;
    }

    private JsonObject selectors() {
        if (selectors != null) return selectors;
        synchronized (this) {
            if (selectors != null) return selectors;
            selectors = JsonParser.parseString(loadResource("/js/chatgpt_selectors.json")).getAsJsonObject();
            return selectors;
        }
    }

    private String loadScript(String resourcePath) {
        if ("/js/inject_prompt.js".equals(resourcePath)) {
            if (injectScript == null) injectScript = loadResource(resourcePath);
            return injectScript;
        }
        if ("/js/trigger_send.js".equals(resourcePath)) {
            if (sendScript == null) sendScript = loadResource(resourcePath);
            return sendScript;
        }
        if ("/js/extract_response.js".equals(resourcePath)) {
            if (extractScript == null) extractScript = loadResource(resourcePath);
            return extractScript;
        }
        if ("/js/fetch_response.js".equals(resourcePath)) {
            if (fetchScript == null) fetchScript = loadResource(resourcePath);
            return fetchScript;
        }
        return loadResource(resourcePath);
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource " + path + ": " + e.getMessage(), e);
        }
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
