package com.github.loki4j.logback.json;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MdcJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MDC_PREFIX = "mdc_";

    /**
     * An exclusive set of MDC keys to include into JSON payload
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
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        var firstFieldWritten = false;
        for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            // skip empty records
            if (entry.getKey() == null || entry.getValue() == null)
                continue;

            // check include list, if defined
            if (!includeKeys.isEmpty() && !includeKeys.contains(entry.getKey()))
                continue;

            if (firstFieldWritten)
                writer.writeFieldSeparator();
            writer.writeStringField(getFieldName() + entry.getKey(), entry.getValue());
            firstFieldWritten = true;
        }
    }

    public void addInclude(String key) {
        includeKeys.add(key);
    }
}
