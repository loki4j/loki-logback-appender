package com.github.loki4j.common;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/**
 * A component that is responsible for splitting a stream of log events into batches.
 * The batch is cut based on the following criteria:
 * <ul>
 * <li> {@code maxItems} - if number of records reaches this limit
 *
 * <li> {@code maxSizeBytes} - if size in bytes (as counted by Loki) reaches this limit,
 * applies only when {@code checkSizeBeforeAdd()} is called
 *
 * <li> {@code maxTimeoutMs} - if this timeout is passed since the last batch was sended,
 * applies only when {@code drain()} is called
 * </ul>
 */
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

    /**
     * Loki limits max message size in bytes by comparing its size in uncompressed
     * protobuf format to a value of setting {@code grpc_server_max_recv_msg_size}.
     * <p>
     * So it does not depend on the format Loki4j sends a batch in (json, compressed protobuf).
     * <p>
     * This method tries to estimate the size of the batch as it was in protobuf format
     * without encoding it. For the batching purposes we only need this approximate size
     * never to be less that real size as counted by Loki, otherwise the message will be dropped
     * by Loki.
     */
    private long estimateSizeBytes(LogRecord r, boolean dryRun) {
        long size = r.message.getBytes(StandardCharsets.UTF_8).length + 24;
        if (!labels.contains(r.stream)) {
            size += r.stream.getBytes(StandardCharsets.UTF_8).length + 8;
            if (!dryRun) labels.add(r.stream);
        }
        return size;
    }

    private void cutBatchAndReset(LogRecordBatch destination, BatchCondition condition) {
        destination.initFrom(items, index, labels.size(), condition, sizeBytes);
        index = 0;
        sizeBytes = 0;
        labels.clear();
    }

    /**
     * Checks if given record can be added to batch without exceeding max bytes limit.
     * Note that this method never adds an input record to the batch, you must call {@code add()}
     * for this purpose.
     * <p>
     * If the size of the record itself is more than max bytes limit (i.e. record is invalid)
     * this method returns {@code false} without further processing.
     * <p>
     * If a valid record can not be added to batch without exceeding max bytes limit, batcher
     * returns a completed batch without this record.
     * <p>
     * Otherwise, no action is performed.
     * @param input Log record to check
     * @param destination Resulting batch (if ready)
     * @return Whether the input record is valid (i.e. it's size is not more than max bytes)
     */
    public boolean checkSizeBeforeAdd(LogRecord input, LogRecordBatch destination) {
        var recordSizeBytes = estimateSizeBytes(input, true);
        if (recordSizeBytes > maxSizeBytes)
            return false;

        if (sizeBytes + recordSizeBytes > maxSizeBytes)
            cutBatchAndReset(destination, BatchCondition.MAX_BYTES);
        return true;
    }

    /**
     * Adds given record to batch and returns a batch if max items limit is reached.
     * @param input Log record to add
     * @param destination Resulting batch (if ready)
     */
    public void add(LogRecord input, LogRecordBatch destination) {
        items[index] = input;
        sizeBytes += estimateSizeBytes(input, false);
        if (++index == items.length)
            cutBatchAndReset(destination, BatchCondition.MAX_ITEMS);
    }

    /**
     * Returns a batch if max timeout since the last batch was sended
     * @param lastSentMs Timestamp when the last batch was sended
     * @param destination Resulting batch (if ready)
     */
    public void drain(long lastSentMs, LogRecordBatch destination) {
        final long now = System.currentTimeMillis();
        if (index > 0 && now - lastSentMs > maxTimeoutMs)
            cutBatchAndReset(destination, BatchCondition.DRAIN);
    }

    public int getCapacity() {
        return items.length;
    }

}
