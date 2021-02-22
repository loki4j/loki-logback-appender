package com.github.loki4j.common;

import java.nio.ByteBuffer;
import java.util.HashMap;

public final class Batcher {

    private final int maxSizeBytes;
    private final long maxTimeoutMs;
    private final Integer[] itemOffsets;
    private final ByteBuffer buf;

    private int index = 0;
    private int sizeBytes = 0;
    private HashMap<byte[], Integer> streams = new HashMap<>();


    public Batcher(int maxItems, int maxSizeBytes, long maxTimeoutMs, ByteBuffer buf) {
        this.maxSizeBytes = maxSizeBytes;
        this.maxTimeoutMs = maxTimeoutMs;
        this.itemOffsets = new Integer[maxItems];
        this.buf = buf;
    }

    private long estimateSizeBytes(byte[] stream, int msgLen, boolean dryRun) {
        // +4 - message field id in pb
        long size = msgLen + 4;
        if (!streams.containsKey(stream)) {
            // +24 - constant stream overhead in json
            size += stream.length + 24;
            if (!dryRun) {
                streams.put(stream, buf.position());
                buf.putInt(stream.length);
                buf.put(stream);
            }
        }
        return size;
    }

    private void cutBatchAndReset(LogRecordBatch destination, BatchCondition condition) {
        destination.initFrom(itemOffsets, index, buf.asReadOnlyBuffer(), condition, sizeBytes);
        index = 0;
        sizeBytes = 16; // 16 - constant starter size in json (in pb it's lesser)
        streams.clear();
    }

    public boolean checkSize(byte[] stream, int msgLen, LogRecordBatch destination) {
        var recordSizeBytes = estimateSizeBytes(stream, msgLen, true);
        if (recordSizeBytes > maxSizeBytes)
            return false;

        if (sizeBytes + recordSizeBytes > maxSizeBytes)
            cutBatchAndReset(destination, BatchCondition.MAX_BYTES);
        return true;
    }

    public void add(byte[] stream, long msgTs, byte[] msg, LogRecordBatch destination) {
        itemOffsets[index] = buf.position();
        sizeBytes += estimateSizeBytes(stream, msg.length, false);
        buf.putInt(streams.get(stream));
        buf.putLong(msgTs);
        buf.putInt(msg.length);
        buf.put(msg); // TODO: buffer overflow!
        if (++index == itemOffsets.length)
            cutBatchAndReset(destination, BatchCondition.MAX_ITEMS);
    }

    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs)
            cutBatchAndReset(destination, BatchCondition.DRAIN);
    }

    public int getCapacity() {
        return itemOffsets.length;
    }

}
