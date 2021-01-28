package com.github.loki4j.common;

import java.util.Arrays;

public final class Batcher {

    private final long maxTimeoutMs;
    private final LogRecord[] items;
    private int index = 0;

    public Batcher(int maxItems, long maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    public LogRecord[] add(LogRecord input, LogRecord[] zeroSizeArray) {
        LogRecord[] batch = zeroSizeArray;
        items[index] = input;
        if (++index == items.length) {
            batch = Arrays.copyOf(items, items.length, zeroSizeArray.getClass());
            index = 0;
        }
        return batch;
    }

    public LogRecord[] drain(long lastSentMs, LogRecord[] zeroSizeArray) {
        LogRecord[] batch = zeroSizeArray;

        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs) {
            batch = Arrays.copyOf(items, index, zeroSizeArray.getClass());
            index = 0;
        }
        return batch;
    }

}
