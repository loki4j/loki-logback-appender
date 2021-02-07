package com.github.loki4j.common;

public final class Batcher {

    private final long maxTimeoutMs;
    private final LogRecord[] items;
    private int index = 0;

    public Batcher(int maxItems, long maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    public void add(LogRecord input, LogRecordBatch destination) {
        items[index] = input;
        if (++index == items.length) {
            destination.initFrom(items, items.length);
            index = 0;
        }
    }

    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs) {
            destination.initFrom(items, index);
            index = 0;
        }
    }

    public int getCapacity() {
        return items.length;
    }

}
