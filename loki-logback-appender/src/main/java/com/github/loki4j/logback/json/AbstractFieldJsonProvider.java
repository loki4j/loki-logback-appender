package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;

public abstract class AbstractFieldJsonProvider extends ContextAwareBase implements JsonProvider<ILoggingEvent> {

    private String fieldName;

    private volatile boolean started;
    
    @Override
    public void prepareForDeferredProcessing(ILoggingEvent event) { }

    @Override
    public boolean canWrite(ILoggingEvent event) {
        return true;
    }

    @Override
    public void start() {
        started = true;
    }
    
    @Override
    public void stop() {
        started = false;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
