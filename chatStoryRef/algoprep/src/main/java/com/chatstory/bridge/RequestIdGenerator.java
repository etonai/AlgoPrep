package com.chatstory.bridge;

import java.util.concurrent.atomic.AtomicLong;

public final class RequestIdGenerator {

    private final AtomicLong next = new AtomicLong(1L);

    public long next() {
        return next.getAndIncrement();
    }
}
