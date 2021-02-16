package com.github.loki4j.common;

public final class Batcher {

    private final long maxTimeoutMs;
    private final LogRecord[] items;
    private int index = 0;

    public Batcher(int maxItems, long maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    private void cutBatchAndReset(LogRecordBatch destination, BatchCondition condition) {
        destination.initFrom(items, index, condition);
        index = 0;
    }

    public void add(LogRecord input, LogRecordBatch destination) {
        items[index] = input;
        if (++index == items.length)
            cutBatchAndReset(destination, BatchCondition.MAX_ITEMS);
    }

    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs)
            cutBatchAndReset(destination, BatchCondition.DRAIN);
    }

    public int getCapacity() {
        return items.length;
    }

}
