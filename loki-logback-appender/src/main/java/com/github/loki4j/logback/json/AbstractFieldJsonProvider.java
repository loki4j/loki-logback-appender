package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * An abstract provider that writes a certain aspect of a logging event as a JSON field
 */
public abstract class AbstractFieldJsonProvider extends ContextAwareBase implements JsonProvider<ILoggingEvent> {

    private boolean enabled = true;

    private String fieldName;

    private volatile boolean started;
    
    @Override
    public boolean canWrite(ILoggingEvent event) {
        return true;
    }

    @Override
    public boolean writeTo(JsonEventWriter writer, ILoggingEvent event, boolean startWithSeparator) {
        if (startWithSeparator)
            writer.writeFieldSeparator();
        writeExactlyOneField(writer, event);
        return true;
    }

    /**
     * Write exactly one field into JSON event layout.
     * @param writer JSON writer to use.
     * @param event Current logback event.
     */
    protected abstract void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event);

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

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
