package com.github.loki4j.common;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

public class LogRecordBatch {

    private static final int TS_OFFSET = 4;

    private final Integer[] itemOffsets;

    private long batchId;

    private int itemIndex;
    private int len;
    private ByteBuffer buf;
    private BatchCondition condition;
    private int estimatedSizeBytes;

    LogRecordBatch(int maxItems, int maxSizeBytes) {
        this.itemOffsets = new Integer[maxItems];
        // TODO: buf init
        clear();
    }

    void clear() {
        batchId = System.nanoTime();
        itemIndex = 0;
        len = 0;
        buf.clear();
        condition = null;
        estimatedSizeBytes = 0;
    }

    int writeStream(byte[] stream) {
        var streamId = buf.position();
        buf.putInt(stream.length);
        buf.put(stream);
        return streamId;
    }

    void writeMessage(int streamId, long msgTs, byte[] msg) {
        itemOffsets[itemIndex] = buf.position();
        buf.putInt(streamId);
        buf.putLong(msgTs);
        buf.putInt(msg.length);
        buf.put(msg); // TODO: buffer overflow!
        itemIndex++;
    }

    void publish(BatchCondition condition, int estimatedSizeBytes) {
        this.condition = condition;
        this.estimatedSizeBytes = estimatedSizeBytes;
        this.len = itemIndex;
    }

    public void sort(boolean sortByStream, boolean sortByTime) {
        var comp = generateComparator(sortByStream, sortByTime);
        if (comp != null)
            Arrays.sort(itemOffsets, 0, len, comp);
    }

    private Comparator<Integer> generateComparator(boolean sortByStream, boolean sortByTime) {
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
