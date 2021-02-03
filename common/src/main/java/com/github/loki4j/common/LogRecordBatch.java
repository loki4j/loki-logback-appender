package com.github.loki4j.common;

public class LogRecordBatch {

    public long batchId;

    public LogRecord[] records;

    public void init(LogRecord[] records) {
        this.batchId = System.nanoTime();
        this.records = records;
    }

    @Override
    public String toString() {
        return String.format("#%x (%s records)", batchId, records.length);
    }
}
