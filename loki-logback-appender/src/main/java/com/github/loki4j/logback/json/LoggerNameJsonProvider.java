package com.github.loki4j.logback.json;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggerNameJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_LOGGER_NAME = "logger_name";

    /**
     * Abbreviator that will shorten the logger classname
     */
    private Abbreviator abbreviator = new NoOpAbbreviator();

    public LoggerNameJsonProvider() {
        setFieldName(FIELD_LOGGER_NAME);
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), abbreviator.abbreviate(event.getLoggerName()));
    }

    public void setAbbreviator(Abbreviator abbreviator) {
        this.abbreviator = abbreviator;
    }

    public static class NoOpAbbreviator implements Abbreviator {
        @Override
        public String abbreviate(String in) {
            return in;
        }
    }
}
