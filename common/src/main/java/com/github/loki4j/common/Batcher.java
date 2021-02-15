package com.github.loki4j.common;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public final class Batcher {

    private final int maxSizeBytes;
    private final long maxTimeoutMs;
    private final LogRecord[] items;

    private int index = 0;
    private int sizeBytes = 0;
    private HashSet<String> labels = new HashSet<>();


    public Batcher(int maxItems, int maxSizeBytes, long maxTimeoutMs) {
        this.maxSizeBytes = maxSizeBytes;
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    private long estimateSizeBytes(LogRecord r, boolean dryRun) {
        // +4 - message field id in pb
        long size = r.binMessage.length + 4;
        if (!labels.contains(r.stream)) {
            // *2 - potential escaping in json
            // +24 - constant stream overhead in json
            size += r.stream.getBytes(StandardCharsets.UTF_8).length * 2 + 24;
            if (!dryRun) labels.add(r.stream);
        }
        return size;
    }

    private void cutBatchAndReset(LogRecordBatch destination, BatchCondition condition) {
        destination.initFrom(items, index, condition, sizeBytes);
        index = 0;
        sizeBytes = 16; // 16 - constant starter size in json (in pb it's lesser)
        labels.clear();
    }

    public boolean checkSize(LogRecord input, LogRecordBatch destination) {
        var recordSizeBytes = estimateSizeBytes(input, true);
        if (recordSizeBytes > maxSizeBytes)
            return false;

        if (sizeBytes + recordSizeBytes > maxSizeBytes)
            cutBatchAndReset(destination, BatchCondition.MAX_BYTES);
        return true;
    }

    public void add(LogRecord input, LogRecordBatch destination) {
        items[index] = input;
        sizeBytes += estimateSizeBytes(input, false);
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
