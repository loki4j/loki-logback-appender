package com.github.loki4j.logback.json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class KeyValuePairsJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_KVP_PREFIX = "kvp_";

    /**
     * A set of keys to exclude from JSON payload.
     * Exclude list has a precedence over include list.
     * If not specified, all keys are included.
     */
    private Set<String> excludeKeys = new HashSet<>();

    /**
     * A set of keys to include into JSON payload.
     * If not specified, all keys are included.
     */
    private Set<String> includeKeys = new HashSet<>();

    public KeyValuePairsJsonProvider() {
        setFieldName(FIELD_KVP_PREFIX);
    }

    @Override
    public boolean canWrite(ILoggingEvent event) {
        List<KeyValuePair> kvPairs = event.getKeyValuePairs();
        return kvPairs != null && !kvPairs.isEmpty();
    }

    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        List<KeyValuePair> kvPairs = event.getKeyValuePairs();
        var firstFieldWritten = false;
        for (KeyValuePair entry : kvPairs) {
            // skip empty records
            if (entry.key == null || entry.value == null)
                continue;

            // check exclude list, if defined
            if (!excludeKeys.isEmpty() && excludeKeys.contains(entry.key))
                continue;

            // check include list, if defined
            if (!includeKeys.isEmpty() && !includeKeys.contains(entry.key))
                continue;

            if (startWithSeparator || firstFieldWritten)
                writer.writeFieldSeparator();
            writer.writeObjectField(getFieldName() + entry.key, entry.value);
            firstFieldWritten = true;
        }
        return firstFieldWritten;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        throw new UnsupportedOperationException(
            "KeyValuePairsJsonProvider can write an arbitrary number of fields. `writeExactlyOneField` should never be called for KeyValuePairsJsonProvider.");
    }

    public void addExclude(String key) {
        excludeKeys.add(key);
    }

    public void addInclude(String key) {
        includeKeys.add(key);
    }

}
