package com.github.loki4j.logback.json;

/**
 * A string that will be serialized to JSON as-is, i.e., no quoting and no escaping will be applied.
 * This string has to be a valid JSON expression.
 */
public final class RawJsonString {
    final String value;

    public RawJsonString(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static RawJsonString from(String rawJson) {
        return new RawJsonString(rawJson);
    }
}
