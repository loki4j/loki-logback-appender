package com.github.loki4j.logback.json;

import java.util.List;

import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class KeyValuePairsJsonProvider extends AbstractFieldCollectionJsonProvider<Object, KeyValuePair, List<KeyValuePair>> {

    public static final String FIELD_KVP_PREFIX = "kvp_";

    public KeyValuePairsJsonProvider() {
        setPrefix(FIELD_KVP_PREFIX);
    }

    @Override
    protected List<KeyValuePair> extractEntries(ILoggingEvent event) {
        return event.getKeyValuePairs();
    }

    @Override
    protected String extractKey(KeyValuePair entry) {
        return entry.key;
    }

    @Override
    protected Object extractValue(KeyValuePair entry) {
        return entry.value;
    }

    @Override
    protected void writeField(JsonEventWriter writer, String fieldName, Object fieldValue) {
        writer.writeObjectField(fieldName, fieldValue);
    }
}
