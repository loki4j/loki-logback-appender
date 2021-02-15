package com.github.loki4j.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class LogRecordBatch {

    private long batchId;

    private LogRecord[] records;

    private int len;

    private BatchCondition condition;

    private int estimatedSizeBytes;

    public LogRecordBatch(int capacity) {
        records = new LogRecord[capacity];
        clear();
    }

    public LogRecordBatch(LogRecord[] source) {
        records = new LogRecord[source.length];
        clear();
        initFrom(source, source.length, BatchCondition.UNKNOWN, 0);
    }

    public void initFrom(LogRecord[] source, int len, BatchCondition condition, int estimatedSizeBytes) {
        if (len > records.length)
            throw new RuntimeException(String.format(
                "Source length %s exceeds available capacity %s", len, records.length));
        batchId = System.nanoTime();
        System.arraycopy(source, 0, records, 0, len);
        this.len = len;
        this.condition = condition;
        this.estimatedSizeBytes = estimatedSizeBytes;
    }

    public void clear() {
        batchId = 0;
        len = 0;
        Arrays.setAll(records, i -> null);
        condition = BatchCondition.UNKNOWN;
        estimatedSizeBytes = 0;
    }

    public void sort(Comparator<LogRecord> comp) {
        Arrays.sort(records, 0, len, comp);
    }

    public LogRecord get(int index) {
        Objects.checkIndex(index, len);
        return records[index];
    }

    public long batchId() {
        return batchId;
    }

    public int size() {
        return len;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public int capacity() {
        return records.length;
    }

    public BatchCondition getCondition() {
        return condition;
    }

    public int getEstimatedSizeBytes() {
        return estimatedSizeBytes;
    }

    public LogRecord[] toArray() {
        return Arrays.copyOf(records, len);
    }

    @Override
    public String toString() {
        return String.format(
            "#%x (%s, %s records, est. size %,d bytes)",
            batchId, condition, len, estimatedSizeBytes);
    }
}
