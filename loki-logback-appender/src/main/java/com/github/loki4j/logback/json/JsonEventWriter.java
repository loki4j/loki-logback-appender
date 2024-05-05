package com.github.loki4j.logback.json;

import static com.github.loki4j.pkg.dslplatform.json.RawJsonWriter.*;

import com.github.loki4j.pkg.dslplatform.json.NumberConverter;
import com.github.loki4j.pkg.dslplatform.json.RawJsonWriter;

/**
 * A wrapper around {@link RawJsonWriter} that supports basic high-level write operations
 */
public class JsonEventWriter {
    
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

    public void writeStringField(String fieldName, String value) {
        raw.writeAsciiString(fieldName);
        raw.writeByte(SEMI);
        if (value != null)
            raw.writeString(value);
        else
            raw.writeNull();
    }

    public void writeNumberField(String fieldName, long value) {
        raw.writeAsciiString(fieldName);
        raw.writeByte(SEMI);
        NumberConverter.serialize(value, raw);
    }

    public String toString() {
        return raw.toString();
    }
}
