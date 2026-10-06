package com.chatstory.browser;

import org.cef.browser.CefBrowser;

import java.awt.Component;

/**
 * Thin wrapper around CefBrowser that lets the UI layer stay decoupled from
 * the raw JCEF type.
 */
public class BrowserPanel {

    private final CefBrowser browser;

    public BrowserPanel(CefBrowser browser) {
        this.browser = browser;
    }

    public Component getUIComponent() {
        return browser.getUIComponent();
    }

    public CefBrowser getBrowser() {
        return browser;
    }
}
