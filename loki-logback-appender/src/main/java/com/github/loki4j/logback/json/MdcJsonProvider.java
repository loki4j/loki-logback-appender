package com.github.loki4j.logback.json;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MdcJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MDC_PREFIX = "mdc_";

    /**
     * A set of MDC keys to exclude from JSON payload.
     * Exclude list has a precedence over include list
     */
    private Set<String> excludeKeys = new HashSet<>();

    /**
     * A set of MDC keys to include into JSON payload.
     * Exclude list has a precedence over include list
     */
    private Set<String> includeKeys = new HashSet<>();

    public MdcJsonProvider() {
        setFieldName(FIELD_MDC_PREFIX);
    }

    @Override
    public boolean canWrite(ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        return mdcProperties != null && !mdcProperties.isEmpty();
    }

    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        var firstFieldWritten = false;
        for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            // skip empty records
            if (entry.getKey() == null || entry.getValue() == null)
                continue;

            // check exclude list, if defined
            if (!excludeKeys.isEmpty() && excludeKeys.contains(entry.getKey()))
                continue;

            // check include list, if defined
            if (!includeKeys.isEmpty() && !includeKeys.contains(entry.getKey()))
                continue;

            if (startWithSeparator || firstFieldWritten)
                writer.writeFieldSeparator();
            writer.writeStringField(getFieldName() + entry.getKey(), entry.getValue());
            firstFieldWritten = true;
        }
        return firstFieldWritten;
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        throw new UnsupportedOperationException(
            "MdcJsonProvider can write an arbitrary number of fields. `writeExactlyOneField` should never be called for MdcJsonProvider.");
    }

    public void addExclude(String key) {
        excludeKeys.add(key);
    }

    public void addInclude(String key) {
        includeKeys.add(key);
    }

}
