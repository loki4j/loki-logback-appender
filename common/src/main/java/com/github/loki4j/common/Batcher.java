package com.github.loki4j.common;

import java.util.HashMap;

public final class Batcher {

    private final int maxItems;
    private final int maxSizeBytes;
    private final long maxTimeoutMs;

    private int sizeItems = 0;
    private int sizeBytes = 0;
    private HashMap<byte[], Integer> streams = new HashMap<>();

    private LogRecordBatch batch;


    public Batcher(int maxItems, int maxSizeBytes, long maxTimeoutMs) {
        this.maxItems = maxItems;
        this.maxSizeBytes = maxSizeBytes;
        this.maxTimeoutMs = maxTimeoutMs;
        this.batch = new LogRecordBatch(maxItems, maxSizeBytes);
    }

    private long estimateSizeBytes(byte[] stream, int msgLen, boolean dryRun) {
        // +4 - message field id in pb
        long size = msgLen + 4;
        if (!streams.containsKey(stream)) {
            // +24 - constant stream overhead in json
            size += stream.length + 24;
            if (!dryRun)
                streams.put(stream, batch.writeStream(stream));
        }
        return size;
    }

    private void completeBatch(BatchCondition condition) {
        batch.publish(condition, sizeBytes);
    }

    public boolean checkSize(byte[] stream, int msgLen, LogRecordBatch destination) {
        var recordSizeBytes = estimateSizeBytes(stream, msgLen, true);
        if (recordSizeBytes > maxSizeBytes)
            return false;

        if (sizeBytes + recordSizeBytes > maxSizeBytes)
            completeBatch(BatchCondition.MAX_BYTES);
        return true;
    }

    public void add(byte[] stream, long msgTs, byte[] msg, LogRecordBatch destination) {
        batch.writeMessage(streams.get(stream), msgTs, msg);
        sizeBytes += estimateSizeBytes(stream, msg.length, false);
        if (++sizeItems == maxItems)
            completeBatch(BatchCondition.MAX_ITEMS);
    }

    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (sizeItems > 0 && now - lastSentMs > maxTimeoutMs)
            completeBatch(BatchCondition.DRAIN);
    }

    public boolean hasCapacity() {
        // check the batch is not published yet
        return batch.isEmpty();
    }

    public LogRecordBatch close() {
        var b = batch;
        batch = null;
        return b;
    }

    public void open(LogRecordBatch b) {
        batch = b;
        sizeItems = 0;
        sizeBytes = 16; // 16 - constant starter size in json (in pb it's lesser)
        streams.clear();
    }

}
