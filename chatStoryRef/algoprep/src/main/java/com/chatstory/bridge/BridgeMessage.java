package com.chatstory.bridge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Parsed envelope for all JS→Java messages sent via window.cefQuery.
 *
 * ok default rule:
 *   - absent + type == "error"  → false
 *   - absent + any other type   → true
 *   - present                   → use the field value
 */
public final class BridgeMessage {

    private final String type;
    private final long requestId;
    private final boolean ok;
    private final String text;
    private final String html;
    private final String errorCode;
    private final String message;

    private BridgeMessage(String type, long requestId, boolean ok,
                          String text, String html, String errorCode, String message) {
        this.type = type;
        this.requestId = requestId;
        this.ok = ok;
        this.text = text;
        this.html = html;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static BridgeMessage parse(String json) throws BridgeMessageException {
        if (json == null || json.isBlank()) {
            throw new BridgeMessageException("Message is empty");
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                throw new BridgeMessageException("Message is not a JSON object");
            }
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type") || obj.get("type").isJsonNull()) {
                throw new BridgeMessageException("Missing required field: type");
            }
            String type = obj.get("type").getAsString();
            if (type.isBlank()) {
                throw new BridgeMessageException("Field 'type' must not be blank");
            }

            long requestId = 0L;
            if (obj.has("requestId") && !obj.get("requestId").isJsonNull()) {
                requestId = obj.get("requestId").getAsLong();
            }

            boolean ok;
            if (obj.has("ok") && !obj.get("ok").isJsonNull()) {
                ok = obj.get("ok").getAsBoolean();
            } else {
                ok = !"error".equals(type);
            }

            String text      = getString(obj, "text");
            String html      = getString(obj, "html");
            String errorCode = getString(obj, "errorCode");
            String message   = getString(obj, "message");

            return new BridgeMessage(type, requestId, ok, text, html, errorCode, message);

        } catch (BridgeMessageException e) {
            throw e;
        } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
            throw new BridgeMessageException("Malformed JSON: " + e.getMessage(), e);
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        JsonElement el = obj.get(key);
        return el.isJsonPrimitive() ? el.getAsString() : null;
    }

    public String getType()      { return type; }
    public long   getRequestId() { return requestId; }
    public boolean isOk()        { return ok; }
    public String getText()      { return text; }
    public String getHtml()      { return html; }
    public String getErrorCode() { return errorCode; }
    public String getMessage()   { return message; }

    @Override
    public String toString() {
        return "BridgeMessage{type='" + type + "', requestId=" + requestId
                + ", ok=" + ok + ", errorCode='" + errorCode + "'}";
    }
}
