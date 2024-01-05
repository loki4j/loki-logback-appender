package com.github.loki4j.logback.json;

import java.io.IOException;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MdcJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MDC_PREFIX = "mdc_";

    public MdcJsonProvider() {
        setFieldName(FIELD_MDC_PREFIX);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) throws IOException {
         Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties != null && !mdcProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
                writer.writeStringField(getFieldName() + entry.getKey(), entry.getValue());
            }
        }
    }
    
}
