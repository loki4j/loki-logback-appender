package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogLevelJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_LEVEL = "level";
    
    public LogLevelJsonProvider() {
        setFieldName(FIELD_LEVEL);
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), event.getLevel().toString());
    }
}
