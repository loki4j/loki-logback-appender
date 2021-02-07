package com.github.loki4j.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class LogRecordBatch {

    private long batchId;

    private LogRecord[] records;

    private int len;

    public LogRecordBatch(int capacity) {
        records = new LogRecord[capacity];
        clear();
    }

    public LogRecordBatch(LogRecord[] source) {
        records = new LogRecord[source.length];
        clear();
        initFrom(source, source.length);
    }

    public void initFrom(LogRecord[] source, int len) {
        if (len > records.length)
            throw new RuntimeException(String.format(
                "Source length %s exceeds available capacity %s", len, records.length));
        batchId = System.nanoTime();
        System.arraycopy(source, 0, records, 0, len);
        this.len = len;
    }

    public void sort(Comparator<LogRecord> comp) {
        Arrays.sort(records, 0, len, comp);
    }

    public LogRecord get(int index) {
        Objects.checkIndex(index, len);
        return records[index];
    }

    public void clear() {
        batchId = 0;
        len = 0;
        Arrays.setAll(records, i -> null);
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

    public LogRecord[] toArray() {
        return Arrays.copyOf(records, len);
    }

    @Override
    public String toString() {
        return String.format("#%x (%s records)", batchId, len);
    }


}
