package com.chatstory.bridge;

/** Implemented in DC003. onResponsePartial is a no-op until DC004. */
public interface ResponseListener {
    void onPromptSubmitted(long requestId);
    void onResponsePartial(long requestId, String responseText);
    void onResponseComplete(long requestId, String responseText, String responseHtml);
    void onError(long requestId, String errorCode, String message);
}
