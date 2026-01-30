package com.github.loki4j.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * An abstract provider that writes a single JSON field
 */
public abstract class AbstractFieldJsonProvider extends AbstractJsonProvider {

    /**
     * A JSON field name to use for this provider.
     */
    private String fieldName;

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

    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
