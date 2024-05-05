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

    public void writeFieldSeparator() {
        raw.writeByte(COMMA);
    }

    public void writeObjectField(String fieldName, Object value) {
        serializeFieldName(fieldName);

        // Object is a reference type, first check the value for null
        if (value == null) {
            raw.writeNull();
            return;
        }

        if (value instanceof String)
            raw.writeString((String) value);
        else if (value instanceof Integer)
            NumberConverter.serialize(((Integer) value).longValue(), raw);
        else if (value instanceof Long)
            NumberConverter.serialize((long) value, raw);
        else if (value instanceof Boolean)
            raw.writeBoolean((boolean) value);
        else if (value instanceof Double)
            raw.writeDouble((double) value);
        else if (value instanceof RawJsonString)
            raw.writeRawAscii(((RawJsonString) value).value);
        else
            raw.writeString(value.toString());
    }

    public void writeStringField(String fieldName, String value) {
        serializeFieldName(fieldName);
        // String is a reference type, first check the value for null
        if (value == null)
            raw.writeNull();
        else
            raw.writeString(value);
    }

    public void writeNumericField(String fieldName, long value) {
        serializeFieldName(fieldName);
        NumberConverter.serialize(value, raw);
    }

    private void serializeFieldName(String fieldName) {
        raw.writeString(fieldName);
        raw.writeByte(SEMI);
    }

    public String toString() {
        return raw.toString();
    }

    /**
     * A string that will be serialized to JSON as-is, i.e., no quoting and no escaping will be applied.
     * This string has to be a valid JSON expression.
     */
    public static final class RawJsonString {
        final String value;

        public RawJsonString(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}
