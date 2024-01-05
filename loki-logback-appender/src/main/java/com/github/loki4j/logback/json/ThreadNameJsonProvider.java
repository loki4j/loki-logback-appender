package com.github.loki4j.logback.json;

import java.io.IOException;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadNameJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_THREAD_NAME = "thread_name";
    
    public ThreadNameJsonProvider() {
        setFieldName(FIELD_THREAD_NAME);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) throws IOException {
        writer.writeStringField(getFieldName(), event.getThreadName());
    }
    
}
