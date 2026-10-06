package com.algoprep.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusReporterTest {

    @Test
    void messagesBeforeAttachAreFlushedInOrder() {
        StatusReporter r = new StatusReporter();
        r.report("one");
        r.report("two");

        List<String> got = new ArrayList<>();
        r.attach(got::add);

        assertEquals(List.of("one", "two"), got);
    }

    @Test
    void messagesAfterAttachGoStraightToTheSink() {
        StatusReporter r = new StatusReporter();
        List<String> got = new ArrayList<>();
        r.attach(got::add);

        r.report("later");

        assertEquals(List.of("later"), got);
    }

    @Test
    void flushedMessagesAreNotDeliveredAgainToAReplacementSink() {
        StatusReporter r = new StatusReporter();
        r.report("early");
        r.attach(s -> { });

        List<String> second = new ArrayList<>();
        r.attach(second::add);

        assertEquals(List.of(), second);
    }
}
