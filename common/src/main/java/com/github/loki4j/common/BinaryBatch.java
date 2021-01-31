package com.github.loki4j.common;

public class BinaryBatch {
    
    public long batchId;

    public int recordsCount;

    public byte[] data;

    public static BinaryBatch fromLogRecordBatch(LogRecordBatch batch, byte[] data) {
        var b = new BinaryBatch();
        b.batchId = batch.batchId;
        b.recordsCount = batch.records.length;
        b.data = data;
        return b;
    }

    @Override
    public String toString() {
        return String.format(
            "#%x (%s records, %,d bytes)", batchId, recordsCount, data.length);
    }
}
