package com.github.loki4j.testkit.dummy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;

import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.writer.Writer;

public class StringWriter implements Writer {
    private final ByteBufferFactory bf;
    private ByteBuffer b;
    private int size = 0;

    public StringWriter(int capacity, ByteBufferFactory bufferFactory) {
        bf = bufferFactory;
        b = bf.allocate(capacity);
    }

    public static String batchToString(LogRecordBatch batch) {
        var s = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            var r = batch.get(i);
            s
                .append(Arrays.toString(r.stream.labels))
                .append(StringPayload.LABELS_MESSAGE_SEPARATOR)
                .append(Arrays.toString(r.metadata))
                .append(StringPayload.LABELS_MESSAGE_SEPARATOR)
                .append("ts=")
                .append(r.timestampMs)
                .append(" ")
                .append(r.message)
                .append('\n');
        }
        return s.toString();
    }

    @Override
    public void serializeBatch(LogRecordBatch batch) {
        b.clear();
        var str = batchToString(batch);
        var data = str.getBytes(StandardCharsets.UTF_8);
        if (b.capacity() < data.length)
            b = bf.allocate(data.length);
        b.put(data);
        b.flip();
        size = data.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void toByteBuffer(ByteBuffer buffer) {
        buffer.put(b);
        buffer.flip();
    }

    @Override
    public byte[] toByteArray() {
        byte[] r = new byte[b.remaining()];
        b.get(r);
        return r;
    }

    @Override
    public void reset() {
        size = 0;
        b.clear();
    }

    @Override
    public boolean isBinary() {
        return false;
    }
}