package com.github.loki4j.logback.json;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MdcJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MDC_PREFIX = "mdc_";

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
            if (firstFieldWritten)
                writer.writeFieldSeparator();
            writer.writeStringField(getFieldName() + entry.getKey(), entry.getValue());
            firstFieldWritten = true;
        }
    }
    
}
