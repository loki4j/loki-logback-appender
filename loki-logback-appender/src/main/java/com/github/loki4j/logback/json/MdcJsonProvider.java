package com.github.loki4j.logback.json;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MdcJsonProvider extends AbstractFieldCollectionJsonProvider<String, Map.Entry<String, String>, Set<Map.Entry<String, String>>> {

    public static final String FIELD_MDC_PREFIX = "mdc_";

    public MdcJsonProvider() {
        setPrefix(FIELD_MDC_PREFIX);
    }

    @Override
    protected Set<Entry<String, String>> extractEntries(ILoggingEvent event) {
        var mdcProperties = event.getMDCPropertyMap();
        return mdcProperties == null ? null : mdcProperties.entrySet();
    }

    @Override
    protected String extractKey(Entry<String, String> entry) {
        return entry.getKey();
    }

    @Override
    protected String extractValue(Entry<String, String> entry) {
        return entry.getValue();
    }

    @Override
    protected void writeField(JsonEventWriter writer, String fieldName, String fieldValue) {
        writer.writeStringField(fieldName, fieldValue);
    }
}
