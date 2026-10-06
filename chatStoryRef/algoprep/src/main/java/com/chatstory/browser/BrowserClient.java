package com.chatstory.browser;

import com.chatstory.AppState;
import com.chatstory.UiThread;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest;

/**
 * Translates JCEF page-load events into AppState transitions and DomBridge pings.
 * All AppState calls are dispatched through UiThread so listeners can safely
 * update Swing components.
 */
public class BrowserClient extends CefLoadHandlerAdapter {

    private final AppState appState;
    private final DomBridge domBridge;

    public BrowserClient(AppState appState, DomBridge domBridge) {
        this.appState  = appState;
        this.domBridge = domBridge;
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame,
                            CefRequest.TransitionType transitionType) {
        if (!frame.isMain()) return;
        String url = browser.getURL();
        UiThread.run(() -> appState.browserLoadStarted(url));
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        if (!frame.isMain()) return;
        String url = browser.getURL();
        boolean loggedIn = !isLoginPage(url);
        UiThread.run(() -> appState.browserLoadFinished(url, loggedIn));
        // Ping fires on every main-frame load — repeated pings during redirects are normal
        domBridge.executePing(browser);
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame,
                            CefLoadHandler.ErrorCode errorCode,
                            String errorText, String failedUrl) {
        if (!frame.isMain()) return;
        String desc = errorCode + ": " + errorText;
        UiThread.run(() -> appState.browserLoadError(failedUrl, desc));
    }

    // Best-effort heuristic — NeedsLogin is informational only in DC002
    private boolean isLoginPage(String url) {
        if (url == null) return false;
        return url.contains("/auth/")
                || url.contains("/login")
                || url.contains("accounts.google.com");
    }
}
