package com.github.loki4j.logback.json;

import java.util.List;

import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class KeyValuePairsJsonProvider extends AbstractFieldCollectionJsonProvider<Object, KeyValuePair, List<KeyValuePair>> {

    public static final String FIELD_KVP_PREFIX = "kvp_";

    /**
     * Json serializer to use for each field specified for this KVP provider.
     */
    private JsonFieldSerializer<Object> fieldSerializer = (writer, name, value) -> writer.writeObjectField(name, value);

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
        fieldSerializer.writeField(writer, fieldName, fieldValue);
    }

    public JsonFieldSerializer<Object> getFieldSerializer() {
        return fieldSerializer;
    }

    public void setFieldSerializer(JsonFieldSerializer<Object> fieldSerializer) {
        this.fieldSerializer = fieldSerializer;
    }
}
