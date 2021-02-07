package com.github.loki4j.common;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class Batcher {

    private static final double SIZE_THRESHOLD = 0.8;

    private final int maxSizeBytes;
    private final long maxTimeoutMs;
    private final LogRecord[] items;

    private int index = 0;
    private int sizeBytes = 0;
    private Set<String> labels = new HashSet<>();


    public Batcher(int maxItems, int maxSizeBytes, long maxTimeoutMs) {
        this.maxSizeBytes = (int)(maxSizeBytes * SIZE_THRESHOLD);
        this.maxTimeoutMs = maxTimeoutMs;
        this.items = new LogRecord[maxItems];
    }

    private long calcSizeBytes(LogRecord r) {
        long size = r.binMessage.length;
        if (!labels.contains(r.stream)) {
            size += r.stream.getBytes(StandardCharsets.UTF_8).length;
            labels.add(r.stream);
        }
        return size;
    }

    private void cutBatchAndReset(LogRecordBatch destination) {
        destination.initFrom(items, index);
        index = 0;
        sizeBytes = 0;
        labels.clear();
    }

    public void add(LogRecord input, LogRecordBatch destination) {
        var recordSizeBytes = calcSizeBytes(input);
        if (sizeBytes + recordSizeBytes > maxSizeBytes)
            cutBatchAndReset(destination);

        items[index] = input;
        sizeBytes += recordSizeBytes;
        // TODO: what if capacity is 1?
        if (++index == items.length)
            cutBatchAndReset(destination);
    }

    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs)
            cutBatchAndReset(destination);
    }

    public int getCapacity() {
        return items.length;
    }

}
