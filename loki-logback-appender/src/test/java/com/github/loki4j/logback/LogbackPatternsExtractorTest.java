package com.github.loki4j.logback;

import static org.junit.Assert.assertArrayEquals;

import java.util.List;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ScanException;


public class LogbackPatternsExtractorTest {

    @Test
    public void testExtract() throws ScanException {
        var extractor = new LogbackPatternsExtractor<ILoggingEvent>(
                List.of("test", "%level", "%thread", "%logger{8}"),
                new LoggerContext());

        var actEv0 = extractor.extract(Loki4jAppenderTest.events[0]);
        assertArrayEquals(new String[] { "test", "INFO", "thread-1", "t.TestApp" }, actEv0);

        var actEv1 = extractor.extract(Loki4jAppenderTest.events[1]);
        assertArrayEquals(new String[] { "test", "WARN", "thread-2", "t.TestApp" }, actEv1);

        var actEv2 = extractor.extract(Loki4jAppenderTest.events[2]);
        assertArrayEquals(new String[] { "test", "INFO", "thread-1", "t.TestApp" }, actEv2);
    }
}
