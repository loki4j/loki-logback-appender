package com.github.loki4j.logback.json;

import static com.github.loki4j.pkg.dslplatform.json.RawJsonWriter.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.loki4j.pkg.dslplatform.json.NumberConverter;
import com.github.loki4j.pkg.dslplatform.json.RawJsonWriter;

/**
 * A wrapper around {@link RawJsonWriter} that supports basic high-level write operations
 */
public final class JsonEventWriter {
    
    private final RawJsonWriter raw;

    public JsonEventWriter(int initialCapacity) {
        raw = new RawJsonWriter(initialCapacity);
    }

    public void writeBeginObject() {
        raw.writeByte(OBJECT_START);
    }

    public void writeEndObject() {
        raw.writeByte(OBJECT_END);
    }

    public void writeFieldSeparator() {
        raw.writeByte(COMMA);
    }

    public void writeObjectField(String fieldName, Object value) {
        writeFieldName(fieldName);
        writeObjectValue(value);
    }

    public void writeCustomField(String fieldName, Consumer<JsonEventWriter> write) {
        writeFieldName(fieldName);
        write.accept(this);
    }

    public void writeStringField(String fieldName, String value) {
        writeFieldName(fieldName);
        writeStringValue(value);
    }

    public void writeNumericField(String fieldName, long value) {
        writeFieldName(fieldName);
        writeNumericValue(value);
    }

    public <T> void writeArrayField(String fieldName, T[] values) {
        writeArrayField(fieldName, values, (w, o) -> w.writeObjectValue(o));
    }

    public <T> void writeArrayField(String fieldName, T[] values, BiConsumer<JsonEventWriter, T> write) {
        writeArrayField(fieldName, Arrays.asList(values), write);
    }

    public <T> void writeArrayField(String fieldName, Iterable<T> values) {
        writeArrayField(fieldName, values, (w, o) -> w.writeObjectValue(o));
    }

    public <T> void writeArrayField(String fieldName, Iterable<T> values, BiConsumer<JsonEventWriter, T> write) {
        writeFieldName(fieldName);
        writeIteratorValue(values.iterator(), write);
    }

    public void writeRawJsonField(String fieldName, String rawJson) {
        writeFieldName(fieldName);
        writeObjectValue(new RawJsonString(rawJson));
    }

    private void writeFieldName(String fieldName) {
        raw.writeString(fieldName);
        raw.writeByte(SEMI);
    }

    private void writeObjectValue(Object value) {
        // Object is a reference type, first check the value for null
        if (value == null) {
            raw.writeNull();
            return;
        }

        if (value instanceof String)
            raw.writeString((String) value);
        else if (value instanceof Integer)
            writeNumericValue(((Integer) value).longValue());
        else if (value instanceof Long)
            writeNumericValue((long) value);
        else if (value instanceof Boolean)
            raw.writeBoolean((boolean) value);
        else if (value instanceof Iterator<?>)
            writeIteratorValue((Iterator<?>) value, (w, o) -> w.writeObjectValue(o));
        else if (value instanceof Iterable)
            writeIteratorValue(((Iterable<?>) value).iterator(), (w, o) -> w.writeObjectValue(o));
        else if (value instanceof RawJsonString)
            raw.writeRawAscii(((RawJsonString) value).value);
        else
            raw.writeString(value.toString());
    }

    private void writeStringValue(String value) {
        // String is a reference type, first check the value for null
        if (value == null) {
            raw.writeNull();
        } else {
            raw.writeString(value);
        }
    }

    private void writeNumericValue(long value) {
        NumberConverter.serialize(value, raw);
    }

    private <T> void writeIteratorValue(Iterator<T> it, BiConsumer<JsonEventWriter, T> write) {
        writeBeginArray();
        while (it.hasNext()) {
            write.accept(this, it.next());
            if (it.hasNext())
                writeArraySeparator();
        }
        writeEndArray();
    }

    private void writeBeginArray() {
        raw.writeByte(ARRAY_START);
    }

    private void writeEndArray() {
        raw.writeByte(ARRAY_END);
    }

    private void writeArraySeparator() {
        raw.writeByte(COMMA);
    }

    public String toString() {
        return raw.toString();
    }
}
