package com.chatstory.bridge;

public interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
    void sendRawPrompt(String prompt, ResponseListener listener);
    void reset();
    void clickUploadFile();
}
