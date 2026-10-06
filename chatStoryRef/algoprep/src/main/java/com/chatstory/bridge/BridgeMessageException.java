package com.chatstory.bridge;

public class BridgeMessageException extends Exception {

    public BridgeMessageException(String message) {
        super(message);
    }

    public BridgeMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
