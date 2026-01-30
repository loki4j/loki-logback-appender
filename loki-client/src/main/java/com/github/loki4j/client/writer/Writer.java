package com.github.loki4j.client.writer;

import java.nio.ByteBuffer;

import com.github.loki4j.client.batch.LogRecordBatch;

public interface Writer {

    boolean isBinary();

    void serializeBatch(LogRecordBatch batch);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    void toByteBuffer(ByteBuffer buffer);

    byte[] toByteArray();

    void reset();
}
