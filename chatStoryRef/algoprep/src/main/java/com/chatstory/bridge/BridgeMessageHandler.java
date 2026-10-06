package com.chatstory.bridge;

import org.cef.callback.CefQueryCallback;

@FunctionalInterface
public interface BridgeMessageHandler {
    void handle(BridgeMessage message, CefQueryCallback callback);
}
