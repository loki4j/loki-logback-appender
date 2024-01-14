package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggerNameJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_LOGGER_NAME = "logger_name";

    public LoggerNameJsonProvider() {
        setFieldName(FIELD_LOGGER_NAME);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), event.getLoggerName());
    }
    
}
