package com.github.loki4j.common;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class Batcher {

    private static final double SIZE_THRESHOLD = 0.8;

    private final int maxSizeBytes;

    private final long maxTimeoutMs;

    private final Queue<LogRecord> source;

    private final EncoderFunction encoder;


    private final LogRecord[] items;

    private final Set<String> labels;

    private int index = 0;

    private int sizeBytes = 0;


    public Batcher(
            int maxItems,
            int maxSizeBytes,
            long maxTimeoutMs,
            Queue<LogRecord> source,
            EncoderFunction encoder) {
        this.maxSizeBytes = (int)(maxSizeBytes * SIZE_THRESHOLD);
        this.maxTimeoutMs = maxTimeoutMs;
        this.source = source;
        this.encoder = encoder;

        this.items = new LogRecord[maxItems];
        this.labels = new HashSet<>();
    }

    private long calcSizeBytes(LogRecord r) {
        long size = r.message.length();
        if (!labels.contains(r.stream)) {
            size += r.stream.length();
            labels.add(r.stream);
        }
        return size;
    }

    private boolean readToLimits() {
        var nextRecord = source.peek();
        while(nextRecord != null) {
            var recordSizeBytes = calcSizeBytes(nextRecord);
            if (sizeBytes + recordSizeBytes > maxSizeBytes)
                return true;
            
            items[index] = nextRecord;
            source.remove();
            if (++index == items.length)
                return true;

            nextRecord = source.peek();
        }
        return false;
    }

    private int encode(byte[] batch) {
        var written = encoder.encode(items, index, batch);

        // reset
        labels.clear();
        index = 0;
        sizeBytes = 0;

        return written;
    }

    public int poll(byte[] batch) {
        if (!readToLimits() || index == 0)
            return 0;
        
        return encode(batch);
    }

    public int drain(long lastSentMs, byte[] batch) {
        if (System.currentTimeMillis() - lastSentMs < maxTimeoutMs)
            return 0;

        readToLimits();

        if (index == 0)
            return 0;

        return encode(batch);
    }

}
