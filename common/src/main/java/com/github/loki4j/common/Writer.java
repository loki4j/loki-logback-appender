package com.github.loki4j.common;

import java.nio.ByteBuffer;

public interface Writer {

    void serializeBatch(LogRecordBatch batch);

    int size();

    void toByteBuffer(ByteBuffer buffer);

    byte[] toByteArray();
}
