package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class MessageJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MESSAGE = "message";

    public MessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), event.getMessage());
    }
    
}
