package com.github.loki4j.common;

import java.nio.ByteBuffer;

public class BinaryBatch {
    
    public long batchId;

    public int sizeItems;

    public int sizeBytes;

    public ByteBuffer data;

    BinaryBatch() { }

    @Override
    public String toString() {
        return String.format(
            "#%x (%,d bytes)", batchId, sizeBytes);
    }

}
