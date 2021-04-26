package com.github.loki4j.common;

import java.nio.ByteBuffer;

public interface Writer {

    void serializeBatch(LogRecordBatch batch);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void toByteBuffer(ByteBuffer buffer);

    byte[] toByteArray();

    void reset();
}
