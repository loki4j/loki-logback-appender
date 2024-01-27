package com.github.loki4j.logback.json;


import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadNameJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_THREAD_NAME = "thread_name";
    
    public ThreadNameJsonProvider() {
        setFieldName(FIELD_THREAD_NAME);
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), event.getThreadName());
    }
    
}
