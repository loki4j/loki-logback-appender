package com.github.loki4j.common;

import java.nio.ByteBuffer;

import com.dslplatform.json.RawJsonWriter;

import static com.dslplatform.json.RawJsonWriter.*;

public final class JsonWriter implements Writer {

    private final RawJsonWriter raw;

    public JsonWriter(int capacity) {
        this.raw = new RawJsonWriter(capacity);
    }

    public void serializeBatch(LogRecordBatch batch) {
        var currentStream = batch.get(0).stream;
        beginStreams(batch.get(0), currentStream.labels);
        for (int i = 1; i < batch.size(); i++) {
            if (batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                nextStream(batch.get(i), currentStream.labels);
            }
            else {
                nextRecord(batch.get(i));
            }
        }
        endStreams();
    }

    public int size() {
        return raw.size();
    }

    public void toByteBuffer(ByteBuffer buffer) {
        raw.toByteBuffer(buffer);
    }

    public byte[] toByteArray() {
        return raw.toByteArray();
    }

    private void beginStreams(LogRecord firstRecord, String[] firstLabels) {
        raw.writeByte(OBJECT_START);
        raw.writeAsciiString("streams");
        raw.writeByte(SEMI);
        raw.writeByte(ARRAY_START);
        stream(firstRecord, firstLabels);
    }

    private void nextStream(LogRecord firstRecord, String[] labels) {
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
        raw.writeByte(COMMA);
        stream(firstRecord, labels);
    }

    private void stream(LogRecord firstRecord, String[] labels) {
        raw.writeByte(OBJECT_START);
        raw.writeAsciiString("stream");
        raw.writeByte(SEMI);
        labels(labels);
        raw.writeByte(COMMA);
        raw.writeAsciiString("values");
        raw.writeByte(SEMI);
        raw.writeByte(ARRAY_START);
        record(firstRecord);
    }

    private void labels(String[] labels) {
        raw.writeByte(OBJECT_START);
        if (labels.length > 0) {
            for (int i = 0; i < labels.length; i+=2) {
                raw.writeString(labels[i]);
                raw.writeByte(SEMI);
                raw.writeString(labels[i + 1]);
                if (i < labels.length - 2)
                    raw.writeByte(COMMA);
            }
        }
        raw.writeByte(OBJECT_END);
    }

    private void nextRecord(LogRecord record) {
        raw.writeByte(COMMA);
        record(record);
    }

    private void record(LogRecord record) {
        raw.writeByte(ARRAY_START);
        raw.writeAsciiString(String.format("%s%06d", record.timestampMs, 0));
        raw.writeByte(COMMA);
        raw.writeString(record.message);
        raw.writeByte(ARRAY_END);
    }

    private void endStreams() {
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
    }

}
