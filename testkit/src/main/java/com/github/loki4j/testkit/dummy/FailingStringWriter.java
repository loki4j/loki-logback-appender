package com.github.loki4j.testkit.dummy;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.util.ByteBufferFactory;

public class FailingStringWriter extends StringWriter {
    public AtomicBoolean fail = new AtomicBoolean(false);

    public FailingStringWriter(int capacity, ByteBufferFactory bufferFactory) {
        super(capacity, bufferFactory);
    }

    @Override
    public void serializeBatch(LogRecordBatch batch) {
        if (fail.get())
            throw new RuntimeException("Text exception");
        super.serializeBatch(batch);
    }
}