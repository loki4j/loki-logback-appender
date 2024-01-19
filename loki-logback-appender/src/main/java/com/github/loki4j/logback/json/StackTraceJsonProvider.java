package com.github.loki4j.logback.json;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class StackTraceJsonProvider extends AbstractFieldJsonProvider {

    public static final String FIELD_STACK_TRACE = "stack_trace";

    /**
     * Used to format throwables as Strings.
     *
     * Uses an {@link ExtendedThrowableProxyConverter} by default.
     */
    private ThrowableHandlingConverter throwableConverter = new ExtendedThrowableProxyConverter();

    public StackTraceJsonProvider() {
        setFieldName(FIELD_STACK_TRACE);
    }

    @Override
    public void start() {
        this.throwableConverter.start();
        super.start();
    }

    @Override
    public void stop() {
        this.throwableConverter.stop();
        super.stop();
    }

    @Override
    public void writeTo(JsonEventWriter writer, ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            writer.writeStringField(getFieldName(), throwableConverter.convert(event));
        }
    }

    public ThrowableHandlingConverter getThrowableConverter() {
        return throwableConverter;
    }

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }
}