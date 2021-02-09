package com.github.loki4j.common;

import java.util.Arrays;
import java.util.HashMap;

public class Batcher2 {

    private final int maxItems;
    private final int maxSizeBytes;
    private final long maxTimeoutMs;

    private int sizeBytes = 0;
    private HashMap<String, Stream> streams = new HashMap<>();


    public Batcher2(int maxItems, long maxTimeoutMs) {

    }

    public boolean add(LogRecord input, BinaryBatch batch) {

    }

    public void drain(long lastSentMs, BinaryBatch batch) {

    }

    private class Stream {
        private long[] timestamps = new long[100];
        private int[] lengths = new int[100];
        private byte[] data = new byte[400];

        private int itemIndex = 0;
        private int cursor = 0;

        public void add(long ts, byte[] record) {
            var len = record.length;
            var insertIndex = itemIndex;
            var insertCursor = cursor;
            while(insertIndex > 0 && timestamps[insertIndex - 1] > ts) {
                insertCursor -= lengths[insertIndex - 1];
                insertIndex--;
            }
            if (insertIndex != itemIndex) {
                System.arraycopy(
                    timestamps, insertIndex, timestamps, insertIndex + 1, itemIndex - insertIndex);
                System.arraycopy(
                    lengths, insertIndex, lengths, insertIndex + 1, itemIndex - insertIndex);
                System.arraycopy(
                    data, insertCursor, data, insertCursor + len, cursor - insertCursor);
            } else {
                System.out.println("no move");
            }
            timestamps[insertIndex] = ts;
            lengths[insertIndex] = len;
            System.arraycopy(record, 0, data, insertCursor, len);
            itemIndex++;
            cursor += len;
            System.out.println(String.format(
                "i=%s, c=%s\nts=%s\nln=%s\nbt%s",
                itemIndex, cursor,
                Arrays.toString(timestamps), Arrays.toString(lengths), Arrays.toString(data)));
        }
    }

}
