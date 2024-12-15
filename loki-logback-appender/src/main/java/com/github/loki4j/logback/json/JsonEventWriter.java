package com.github.loki4j.logback.json;

import static com.github.loki4j.pkg.dslplatform.json.RawJsonWriter.*;

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

    public void writeBeginArray() {
        raw.writeByte(ARRAY_START);
    }

    public void writeEndArray() {
        raw.writeByte(ARRAY_END);
    }

    public void writeFieldSeparator() {
        raw.writeByte(COMMA);
    }

    public void writeArraySeparator() {
        raw.writeByte(COMMA);
    }

    public void writeObjectField(String fieldName, Object value) {
        serializeFieldName(fieldName);
        writeObjectValue(value);
    }

    public void writeObjectValue(Object value) {
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
        else if (value instanceof Iterable)
            serializeIterable((Iterable<?>) value);
        else if (value instanceof RawJsonString)
            raw.writeRawAscii(((RawJsonString) value).value);
        else
            raw.writeString(value.toString());
    }

    public void writeStringField(String fieldName, String value) {
        serializeFieldName(fieldName);
        writeStringValue(value);
    }

    public void writeStringValue(String value) {
        // String is a reference type, first check the value for null
        if (value == null) {
            raw.writeNull();
        } else {
            raw.writeString(value);
        }
    }

    public void writeNumericField(String fieldName, long value) {
        serializeFieldName(fieldName);
        writeNumericValue(value);
    }

    public void writeNumericValue(long value) {
        NumberConverter.serialize(value, raw);
    }

    private void serializeFieldName(String fieldName) {
        raw.writeString(fieldName);
        raw.writeByte(SEMI);
    }

    private void serializeIterable(Iterable<?> iterable) {
        writeBeginArray();
        var it = iterable.iterator();
        while (it.hasNext()) {
            writeObjectValue(it.next());
            if (it.hasNext())
                writeArraySeparator();
        }
        writeEndArray();
    }

    public String toString() {
        return raw.toString();
    }
}
