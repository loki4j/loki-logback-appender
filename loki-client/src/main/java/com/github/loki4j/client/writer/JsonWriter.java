package com.github.loki4j.client.writer;

import static com.github.loki4j.pkg.dslplatform.json.RawJsonWriter.*;

import java.nio.ByteBuffer;
import java.util.Map;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.pkg.dslplatform.json.RawJsonWriter;

public final class JsonWriter implements Writer {

    private final RawJsonWriter raw;

    public JsonWriter(int capacity) {
        this.raw = new RawJsonWriter(capacity);
    }

    public boolean isBinary() {
        return false;
    }

    public void serializeBatch(LogRecordBatch batch) {
        var currentStream = batch.get(0).stream;
        beginStreams(batch.get(0), currentStream);
        for (int i = 1; i < batch.size(); i++) {
            if (!batch.get(i).stream.equals(currentStream)) {
                currentStream = batch.get(i).stream;
                nextStream(batch.get(i), currentStream);
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

    public final void reset() {
        raw.reset();
    }

    private void beginStreams(LogRecord firstRecord, Map<String, String> firstLabels) {
        raw.writeByte(OBJECT_START);
        raw.writeQuotedAscii("streams");
        raw.writeByte(SEMI);
        raw.writeByte(ARRAY_START);
        stream(firstRecord, firstLabels);
    }

    private void nextStream(LogRecord firstRecord, Map<String, String> labels) {
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
        raw.writeByte(COMMA);
        stream(firstRecord, labels);
    }

    private void stream(LogRecord firstRecord, Map<String, String> labels) {
        raw.writeByte(OBJECT_START);
        raw.writeQuotedAscii("stream");
        raw.writeByte(SEMI);
        keyValuePairs(labels);
        raw.writeByte(COMMA);
        raw.writeQuotedAscii("values");
        raw.writeByte(SEMI);
        raw.writeByte(ARRAY_START);
        record(firstRecord);
    }

    private void nextRecord(LogRecord record) {
        raw.writeByte(COMMA);
        record(record);
    }

    private void record(LogRecord record) {
        raw.writeByte(ARRAY_START);
        raw.writeQuotedAscii("" + record.timestampMs + nanosToStr(record.nanosInMs));
        raw.writeByte(COMMA);
        raw.writeString(record.message);
        if (!record.metadata.isEmpty()) {
            raw.writeByte(COMMA);
            keyValuePairs(record.metadata);
        }
        raw.writeByte(ARRAY_END);
    }

    private void keyValuePairs(Map<String, String> kvp) {
        raw.writeByte(OBJECT_START);
        var entries = kvp.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            raw.writeString(entry.getKey());
            raw.writeByte(SEMI);
            raw.writeString(entry.getValue());
            if (entries.hasNext())
                raw.writeByte(COMMA);
        }
        raw.writeByte(OBJECT_END);
    }

    private String nanosToStr(int nanos) {
        var c = new char[6];
        var rem = nanos;
        for (int i = c.length - 1; i >= 0 ; i--) {
            c[i] = (char)('0' + rem % 10);
            rem = rem / 10;
        }
        return new String(c);
    }

    private void endStreams() {
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
        raw.writeByte(ARRAY_END);
        raw.writeByte(OBJECT_END);
    }

}
