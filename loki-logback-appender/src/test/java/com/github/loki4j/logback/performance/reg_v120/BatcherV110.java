package com.github.loki4j.logback.performance.reg_v120;

import com.github.loki4j.common.batch.BatchCondition;
import com.github.loki4j.common.batch.LogRecord;
import com.github.loki4j.common.batch.LogRecordBatch;

public final class BatcherV110 {

    private final long maxTimeoutMs;
    private final LogRecord[] items;
    private int index = 0;

    public BatcherV110(int maxItems, long maxTimeoutMs) {
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    private void cutBatchAndReset(LogRecordBatch destination, BatchCondition condition) {
        destination.initFrom(items, index, 0, condition, 0);
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
