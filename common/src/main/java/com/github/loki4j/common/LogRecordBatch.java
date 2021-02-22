package com.github.loki4j.common;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class LogRecordBatch {

    private static final int TS_OFFSET = 4;

    private final boolean sortByStream;

    private final boolean sortByTime;

    private long batchId;
    private Integer[] itemOffsets;
    private int len;
    private ByteBuffer buf;
    private BatchCondition condition;
    private int estimatedSizeBytes;

    public LogRecordBatch(boolean sortByStream, boolean sortByTime) {
        this.sortByStream = sortByStream;
        this.sortByTime = sortByTime;

        condition = BatchCondition.UNKNOWN;
        // all other fields are initialized with their default values
    }

    void initFrom(Integer[] itemOffsets, int len, ByteBuffer buf, BatchCondition condition, int estimatedSizeBytes) {
        this.itemOffsets = itemOffsets;
        this.len = len;
        this.buf = buf;
        this.condition = condition;
        this.estimatedSizeBytes = estimatedSizeBytes;

        batchId = System.nanoTime();
        sort();
    }

    private void sort() {
        var comp = generateComparator();
        if (comp != null)
            Arrays.sort(itemOffsets, 0, len, comp);
    }

    private Comparator<Integer> generateComparator() {
        if (sortByStream) {
            Comparator<Integer> byStream = (i1, i2) -> 
                Integer.compare(buf.getInt(i1), buf.getInt(i2));
            if (sortByTime) {
                Comparator<Integer> byTime = (i1, i2) -> 
                    Long.compare(buf.getLong(i1 + TS_OFFSET), buf.getLong(i2 + TS_OFFSET));
                return byStream.thenComparing(byTime);
            } else {
                return byStream;
            }
        } else if (sortByTime) {
            Comparator<Integer> byTime = (i1, i2) -> 
                Long.compare(buf.getLong(i1 + TS_OFFSET), buf.getLong(i2 + TS_OFFSET));
            return byTime;
        } else {
            return null;
        }
     }

    public int getStreamId(int index) {
        if (index < 0 || index >= len)
            throw new IndexOutOfBoundsException("LogRecordBatch: Index out of range: " + index);
        return buf.getInt(itemOffsets[index]);
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
        return itemOffsets.length;
    }

    public BatchCondition getCondition() {
        return condition;
    }

    public int getEstimatedSizeBytes() {
        return estimatedSizeBytes;
    }

    /*public LogRecord[] toArray() {
        return Arrays.copyOf(records, len);
    }*/

    @Override
    public String toString() {
        return String.format(
            "#%x (%s, %s records, est. size %,d bytes)",
            batchId, condition, len, estimatedSizeBytes);
    }
}
