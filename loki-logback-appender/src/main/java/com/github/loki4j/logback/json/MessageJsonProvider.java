package com.github.loki4j.logback.json;

import java.io.IOException;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MessageJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_MESSAGE = "message";

    public MessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) throws IOException {
        writer.writeStringField(getFieldName(), event.getMessage());
    }
    
}
