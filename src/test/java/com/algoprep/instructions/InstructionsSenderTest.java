package com.algoprep.instructions;

import com.algoprep.bridge.ChatBridge;
import com.algoprep.bridge.ErrorCodes;
import com.algoprep.bridge.ResponseListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InstructionsSenderTest {

    @TempDir
    Path dir;

    /** Records calls; the test completes the send by calling the captured listener. */
    private static final class FakeBridge implements ChatBridge {
        final List<String> calls = new ArrayList<>();
        String rawPrompt;
        ResponseListener listener;

        @Override public void sendPrompt(String prompt, ResponseListener l) { calls.add("sendPrompt"); }
        @Override public void sendRawPrompt(String prompt, ResponseListener l) {
            calls.add("sendRawPrompt");
            rawPrompt = prompt;
            listener = l;
        }
        @Override public void reset() { calls.add("reset"); }
        @Override public void clickUploadFile() { calls.add("clickUploadFile"); }
    }

    private Path file(String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }

    // ---- load ----

    @Test
    void loadRejectsUnsetMissingAndBlank() throws IOException {
        assertFalse(InstructionsSender.load(null).ok());
        assertFalse(InstructionsSender.load("  ").ok());
        assertTrue(InstructionsSender.load(dir.resolve("nope.md").toString()).problem().contains("not found"));
        assertTrue(InstructionsSender.load(file("blank.md", " \n\t ").toString()).problem().contains("empty"));
        assertTrue(InstructionsSender.load(dir.toString()).problem().contains("not found"));
    }

    @Test
    void loadReturnsExactText() throws IOException {
        String text = "Line one\n\nLine two with ünïcode\n";
        InstructionsSender.Loaded l = InstructionsSender.load(file("i.md", text).toString());
        assertTrue(l.ok());
        assertEquals(text, l.text());
    }

    @Test
    void loadReportsInvalidUtf8AsUnreadable() throws IOException {
        Path p = dir.resolve("bad.md");
        Files.write(p, new byte[] {(byte) 0xC3, (byte) 0x28});
        InstructionsSender.Loaded l = InstructionsSender.load(p.toString());
        assertFalse(l.ok());
        assertTrue(l.problem().contains("could not be read"));
    }

    // ---- send ----

    @Test
    void sendResetsThenSendsRawTextOnceAndReportsOnlyAfterConfirmation() throws IOException {
        String text = "Be brief.\n";
        FakeBridge bridge = new FakeBridge();
        List<String> status = new ArrayList<>();
        AtomicInteger sent = new AtomicInteger();

        InstructionsSender.send(file("i.md", text).toString(), () -> true, bridge, status::add, sent::incrementAndGet);

        assertEquals(List.of("reset", "sendRawPrompt"), bridge.calls);
        assertEquals(text, bridge.rawPrompt);
        assertTrue(status.isEmpty(), "nothing is reported until the bridge confirms");
        assertEquals(0, sent.get());

        bridge.listener.onPromptSubmitted(1L);

        assertEquals(List.of("Instructions message sent"), status);
        assertEquals(1, sent.get());
    }

    @Test
    void sendReadsTheFileAtEachPress() throws IOException {
        Path p = file("i.md", "first");
        FakeBridge bridge = new FakeBridge();

        InstructionsSender.send(p.toString(), () -> true, bridge, s -> { }, () -> { });
        assertEquals("first", bridge.rawPrompt);

        Files.writeString(p, "second");
        InstructionsSender.send(p.toString(), () -> true, bridge, s -> { }, () -> { });
        assertEquals("second", bridge.rawPrompt);
    }

    @Test
    void unreadableFileNeverTouchesTheBridge() {
        FakeBridge bridge = new FakeBridge();
        List<String> status = new ArrayList<>();

        InstructionsSender.send(dir.resolve("missing.md").toString(), () -> true, bridge, status::add, () -> { });

        assertTrue(bridge.calls.isEmpty());
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("not found"));
    }

    @Test
    void browserNotReadyDoesNotResetOrSend() throws IOException {
        FakeBridge bridge = new FakeBridge();
        List<String> status = new ArrayList<>();

        InstructionsSender.send(file("i.md", "x").toString(), () -> false, bridge, status::add, () -> { });

        assertTrue(bridge.calls.isEmpty(), "reset would force Ready and hide a loading page");
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("not ready"));
    }

    @Test
    void bridgeErrorIsReportedAndNotRetriedAndNotMarkedSent() throws IOException {
        FakeBridge bridge = new FakeBridge();
        List<String> status = new ArrayList<>();
        AtomicInteger sent = new AtomicInteger();

        InstructionsSender.send(file("i.md", "x").toString(), () -> true, bridge, status::add, sent::incrementAndGet);
        bridge.listener.onError(1L, ErrorCodes.USER_MESSAGE_NOT_CONFIRMED, "timed out");

        assertEquals(2, bridge.calls.size(), "no automatic retry");
        assertEquals(0, sent.get());
        assertEquals(1, status.size());
        assertTrue(status.get(0).contains("not confirmed"));
        assertTrue(status.get(0).contains("timed out"));
    }

    // ---- error wording ----

    @Test
    void errorWordingDistinguishesFailureKinds() {
        String notReady = InstructionsSender.describeError(ErrorCodes.SEND_BUTTON_DISABLED, "x");
        String injection = InstructionsSender.describeError(ErrorCodes.PROMPT_INJECTION_FAILED, "boom");
        String click = InstructionsSender.describeError(ErrorCodes.SEND_CLICK_FAILED, null);
        String unconfirmed = InstructionsSender.describeError(ErrorCodes.TIMEOUT, null);
        String other = InstructionsSender.describeError("weird", "m");

        assertTrue(notReady.contains("not ready"));
        assertTrue(injection.contains("input box") && injection.contains("Nothing was sent"));
        assertTrue(click.contains("press Send"));
        assertTrue(unconfirmed.contains("may or may not"));
        assertTrue(other.contains("weird"));
        assertEquals(5, java.util.Set.of(notReady, injection, click, unconfirmed, other).size());
    }

    @Test
    void errorWordingNeverClaimsConfiguredOrAttached() {
        for (String code : List.of(ErrorCodes.SEND_BUTTON_DISABLED, ErrorCodes.EDITOR_NOT_FOUND,
                ErrorCodes.SEND_CLICK_FAILED, ErrorCodes.USER_MESSAGE_NOT_CONFIRMED, "other")) {
            String text = InstructionsSender.describeError(code, "m").toLowerCase();
            assertFalse(text.contains("configured"));
            assertFalse(text.contains("attached"));
        }
    }
}
