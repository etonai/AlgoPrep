package com.chatstory.bridge;

public final class ErrorCodes {

    // Browser / navigation
    public static final String PAGE_LOAD_FAILED           = "page_load_failed";
    public static final String NOT_ON_CHATGPT_PAGE        = "not_on_chatgpt_page";
    public static final String LOGIN_REQUIRED             = "login_required";

    // Bridge protocol
    public static final String BRIDGE_MESSAGE_INVALID     = "bridge_message_invalid";
    public static final String BRIDGE_HANDLER_FAILED      = "bridge_handler_failed";

    // Injection — used in DC003
    public static final String EDITOR_NOT_FOUND           = "editor_not_found";
    public static final String PROMPT_INJECTION_FAILED    = "prompt_injection_failed";
    public static final String SEND_BUTTON_NOT_FOUND      = "send_button_not_found";
    public static final String SEND_BUTTON_DISABLED       = "send_button_disabled";
    public static final String SEND_CLICK_FAILED          = "send_click_failed";
    public static final String USER_MESSAGE_NOT_CONFIRMED = "user_message_not_confirmed";

    // Extraction — used in DC004
    public static final String EXTRACTION_FAILED          = "extraction_failed";
    public static final String TIMEOUT                    = "timeout";

    private ErrorCodes() {}
}
