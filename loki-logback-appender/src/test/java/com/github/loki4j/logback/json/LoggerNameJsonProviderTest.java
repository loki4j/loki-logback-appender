package com.github.loki4j.logback.json;

import static com.github.loki4j.logback.Generators.loggingEvent;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LoggerNameJsonProviderTest {
    
    private static final ILoggingEvent event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);

    @Test
    public void testNoAbbreviation() {
        var provider = new LoggerNameJsonProvider();
        provider.start();

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("\"logger_name\":\"io.test.TestApp\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testClassNameAbbreviation() {
        var provider = new LoggerNameJsonProvider();
        provider.setTargetLength(0);
        provider.start();

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("\"logger_name\":\"TestApp\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testTargetLengthAbbreviation() {
        var provider = new LoggerNameJsonProvider();
        provider.setTargetLength(11);
        provider.start();

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("\"logger_name\":\"i.t.TestApp\"", writer.toString());

        provider.stop();
    }
}
