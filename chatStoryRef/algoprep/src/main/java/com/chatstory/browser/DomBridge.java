package com.chatstory.browser;

import com.chatstory.bridge.BridgeMessage;
import com.chatstory.bridge.BridgeMessageException;
import com.chatstory.bridge.BridgeMessageHandler;
import com.chatstory.bridge.ErrorCodes;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the CefMessageRouter and routes JS→Java messages by type.
 *
 * Dependency direction: browser → bridge (correct).
 * The bridge package never imports from browser.
 */
public class DomBridge {

    private final ConcurrentHashMap<String, BridgeMessageHandler> handlers =
            new ConcurrentHashMap<>();

    private volatile String pingScript;

    public DomBridge(CefClient client) {
        CefMessageRouter.CefMessageRouterConfig config =
                new CefMessageRouter.CefMessageRouterConfig();
        config.jsQueryFunction   = "cefQuery";
        config.jsCancelFunction  = "cefQueryCancel";

        CefMessageRouter router = CefMessageRouter.create(config);
        router.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                                   String request, boolean persistent,
                                   CefQueryCallback callback) {
                dispatch(request, callback);
                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                System.out.println("[DomBridge] Query cancelled id=" + queryId);
            }
        }, true);

        client.addMessageRouter(router);

        // Default ping handler registered at construction
        registerHandler("ping", (msg, cb) -> {
            System.out.println("[Bridge] ping received requestId=" + msg.getRequestId());
            cb.success("ok");
        });
    }

    public void registerHandler(String type, BridgeMessageHandler handler) {
        handlers.put(type, handler);
    }

    /** Execute a fire-and-forget JavaScript command in the given browser. */
    public void execute(CefBrowser browser, String script) {
        browser.executeJavaScript(script, browser.getURL(), 0);
    }

    /** Load ping.js from resources (cached) and execute it. */
    public void executePing(CefBrowser browser) {
        String script = loadPingScript();
        if (!script.isEmpty()) execute(browser, script);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void dispatch(String request, CefQueryCallback callback) {
        BridgeMessage msg;
        try {
            msg = BridgeMessage.parse(request);
        } catch (BridgeMessageException e) {
            System.err.println("[DomBridge] " + ErrorCodes.BRIDGE_MESSAGE_INVALID
                    + ": " + e.getMessage() + " | raw=" + request);
            callback.failure(-1, ErrorCodes.BRIDGE_MESSAGE_INVALID);
            return;
        }

        BridgeMessageHandler handler = handlers.get(msg.getType());
        if (handler == null) {
            System.out.println("[DomBridge] No handler registered for type: " + msg.getType());
            callback.success("");
            return;
        }

        try {
            handler.handle(msg, callback);
        } catch (Exception e) {
            System.err.println("[DomBridge] " + ErrorCodes.BRIDGE_HANDLER_FAILED
                    + " type=" + msg.getType() + ": " + e.getMessage());
            callback.failure(-1, ErrorCodes.BRIDGE_HANDLER_FAILED);
        }
    }

    private String loadPingScript() {
        if (pingScript != null) return pingScript;
        synchronized (this) {
            if (pingScript != null) return pingScript;
            try (InputStream is = getClass().getResourceAsStream("/js/ping.js")) {
                if (is == null) {
                    System.err.println("[DomBridge] ping.js not found in classpath");
                    pingScript = "";
                } else {
                    pingScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                System.err.println("[DomBridge] Failed to load ping.js: " + e.getMessage());
                pingScript = "";
            }
        }
        return pingScript;
    }
}
