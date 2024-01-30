package com.github.loki4j.logback.json;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.pattern.ClassNameOnlyAbbreviator;
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggerNameJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_LOGGER_NAME = "logger_name";

    /**
     * The desired target length:
     * {@code -1} to disable abbreviation,
     * {@code 0} to print class name only,
     * {@code >0} to abbreviate to the target length.
     */
    private int targetLength = -1;

    private Abbreviator abbreviator;

    public LoggerNameJsonProvider() {
        setFieldName(FIELD_LOGGER_NAME);
    }

    @Override
    public void start() {
        super.start();
        if (targetLength > 0)
            abbreviator = new TargetLengthBasedClassNameAbbreviator(targetLength);
        else if (targetLength == 0)
            abbreviator = new ClassNameOnlyAbbreviator();
        else
            abbreviator = new NoOpAbbreviator();
    }

    @Override
    public void stop() {
        abbreviator = null;
        super.stop();
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), abbreviator.abbreviate(event.getLoggerName()));
    }

    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }

    public static class NoOpAbbreviator implements Abbreviator {
        @Override
        public String abbreviate(String in) {
            return in;
        }
    }
}
