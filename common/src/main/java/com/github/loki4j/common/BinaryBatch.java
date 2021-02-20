package com.github.loki4j.common;

import java.util.Arrays;
import java.util.function.Consumer;

public class BinaryBatch {
    
    private long batchId;

    private byte[] data = new byte[0];

    private int len;

    public void initFrom(long batchId, int len, Consumer<byte[]> fillData) {
        this.batchId = batchId;
        this.len = len;
        if (data.length < len)
            data = new byte[len + len / 2];
        fillData.accept(data);
    }

    public long batchId() {
        return batchId;
    }

    public int size() {
        return len;
    }

    public byte[] buffer() {
        return data;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public int capacity() {
        return data.length;
    }

    public byte[] toArray() {
        return Arrays.copyOf(data, len);
    }

    @Override
    public String toString() {
        return String.format(
            "#%x (%,d bytes)", batchId, data.length);
    }
}
