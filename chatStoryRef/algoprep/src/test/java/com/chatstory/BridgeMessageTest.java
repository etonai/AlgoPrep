package com.chatstory;

import com.chatstory.bridge.BridgeMessage;
import com.chatstory.bridge.BridgeMessageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeMessageTest {

    // ---- Complete well-formed message ---------------------------------------

    @Test
    void parsesAllFields() throws Exception {
        BridgeMessage msg = BridgeMessage.parse(
                "{\"type\":\"ping\",\"requestId\":7,\"ok\":true," +
                "\"text\":\"hello\",\"errorCode\":\"ec\",\"message\":\"msg\"}");
        assertEquals("ping", msg.getType());
        assertEquals(7L,     msg.getRequestId());
        assertTrue(msg.isOk());
        assertEquals("hello", msg.getText());
        assertEquals("ec",    msg.getErrorCode());
        assertEquals("msg",   msg.getMessage());
    }

    // ---- Minimal message (type only) ----------------------------------------

    @Test
    void typeOnlyParsesWithDefaults() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\"}");
        assertEquals("ping", msg.getType());
        assertEquals(0L,     msg.getRequestId());
        assertTrue(msg.isOk());  // absent ok, non-error type → true
        assertNull(msg.getText());
        assertNull(msg.getErrorCode());
        assertNull(msg.getMessage());
    }

    // ---- ok default rule ----------------------------------------------------

    @Test
    void okAbsentForNonErrorTypeDefaultsTrue() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\"}");
        assertTrue(msg.isOk());
    }

    @Test
    void okAbsentForErrorTypeDefaultsFalse() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"error\",\"requestId\":1}");
        assertFalse(msg.isOk());
    }

    @Test
    void okExplicitFalseOverridesType() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\",\"ok\":false}");
        assertFalse(msg.isOk());
    }

    @Test
    void okExplicitTrueOnErrorType() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"error\",\"ok\":true}");
        assertTrue(msg.isOk());
    }

    // ---- requestId defaults -------------------------------------------------

    @Test
    void requestIdDefaultsToZeroWhenAbsent() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\"}");
        assertEquals(0L, msg.getRequestId());
    }

    @Test
    void requestIdParsedCorrectly() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\",\"requestId\":42}");
        assertEquals(42L, msg.getRequestId());
    }

    // ---- errorCode and message are independent ------------------------------

    @Test
    void errorCodeNullWhenAbsent() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\"}");
        assertNull(msg.getErrorCode());
    }

    @Test
    void messageNullWhenAbsent() throws Exception {
        BridgeMessage msg = BridgeMessage.parse("{\"type\":\"ping\"}");
        assertNull(msg.getMessage());
    }

    @Test
    void errorCodeAndMessageAreIndependent() throws Exception {
        BridgeMessage msg = BridgeMessage.parse(
                "{\"type\":\"error\",\"errorCode\":\"timeout\",\"message\":null}");
        assertEquals("timeout", msg.getErrorCode());
        assertNull(msg.getMessage());
    }

    // ---- Error cases --------------------------------------------------------

    @Test
    void missingTypeThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse("{\"requestId\":1}"));
    }

    @Test
    void nullTypeThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse("{\"type\":null}"));
    }

    @Test
    void blankTypeThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse("{\"type\":\"\"}"));
    }

    @Test
    void malformedJsonThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse("{not valid json"));
    }

    @Test
    void emptyStringThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse(""));
    }

    @Test
    void nullThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse(null));
    }

    @Test
    void nonObjectJsonThrows() {
        assertThrows(BridgeMessageException.class,
                () -> BridgeMessage.parse("[\"type\",\"ping\"]"));
    }
}
