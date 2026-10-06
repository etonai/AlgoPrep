package com.chatstory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLoadingTest {

    @Test
    void pingJsLoadsAsNonEmptyString() throws IOException {
        String content = loadResource("/js/ping.js");
        assertNotNull(content);
        assertFalse(content.isBlank(), "ping.js should not be empty");
    }

    @Test
    void injectPromptJsLoadsAsNonEmptyString() throws IOException {
        String content = loadResource("/js/inject_prompt.js");
        assertNotNull(content);
        assertFalse(content.isBlank(), "inject_prompt.js should not be empty");
    }

    @Test
    void triggerSendJsLoadsAsNonEmptyString() throws IOException {
        String content = loadResource("/js/trigger_send.js");
        assertNotNull(content);
        assertFalse(content.isBlank(), "trigger_send.js should not be empty");
    }

    @Test
    void extractResponseJsLoadsAsNonEmptyString() throws IOException {
        String content = loadResource("/js/extract_response.js");
        assertNotNull(content);
        assertFalse(content.isBlank(), "extract_response.js should not be empty");
    }

    @Test
    void chatgptSelectorsJsonLoadsAsNonEmptyString() throws IOException {
        String content = loadResource("/js/chatgpt_selectors.json");
        assertNotNull(content);
        assertFalse(content.isBlank(), "chatgpt_selectors.json should not be empty");
    }

    @Test
    void selectorsJsonContainsAllExpectedKeys() throws IOException {
        JsonObject obj = loadJson("/js/chatgpt_selectors.json");
        List<String> required = List.of("promptEditor", "sendButton", "stopButton", "assistantMsg", "userMsg");
        for (String key : required) {
            assertTrue(obj.has(key), "Missing key: " + key);
        }
    }

    @Test
    void selectorsJsonEachKeyIsNonEmptyStringArray() throws IOException {
        JsonObject obj = loadJson("/js/chatgpt_selectors.json");
        for (String key : List.of("promptEditor", "sendButton", "stopButton", "assistantMsg", "userMsg")) {
            JsonElement el = obj.get(key);
            assertTrue(el.isJsonArray(), key + " should be a JSON array");
            JsonArray arr = el.getAsJsonArray();
            assertFalse(arr.isEmpty(), key + " array should not be empty");
            for (JsonElement item : arr) {
                assertTrue(item.isJsonPrimitive() && item.getAsJsonPrimitive().isString(),
                        key + " array element should be a string, got: " + item);
            }
        }
    }

    // ---- Helpers ------------------------------------------------------------

    private String loadResource(String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonObject loadJson(String path) throws IOException {
        return JsonParser.parseString(loadResource(path)).getAsJsonObject();
    }
}
