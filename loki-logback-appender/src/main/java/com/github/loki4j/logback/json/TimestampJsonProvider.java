package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class TimestampJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_TIMESTAMP = "timestamp_ms";

    public TimestampJsonProvider() {
        setFieldName(FIELD_TIMESTAMP);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeNumberField(getFieldName(), event.getTimeStamp());
    }
    
}
