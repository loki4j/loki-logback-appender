package com.github.loki4j.logback.json;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogLevelJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_LEVEL = "level";
    
    public LogLevelJsonProvider() {
        setFieldName(FIELD_LEVEL);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) throws IOException {
        writer.writeStringField(getFieldName(), event.getLevel().toString());
    }
}
