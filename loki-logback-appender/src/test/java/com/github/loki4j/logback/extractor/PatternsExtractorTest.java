package com.github.loki4j.logback.extractor;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.github.loki4j.logback.Loki4jAppenderTest;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.ScanException;

public class PatternsExtractorTest {

    @Test
    public void testExtract() throws ScanException {
        var extractor = new PatternsExtractor(
                Map.of("app", "test", "lvl", "%level", "thread", "%thread", "class", "%logger{8}"),
                new LoggerContext());

        var kvs = new LinkedHashMap<String, String>();
        extractor.extract(Loki4jAppenderTest.events[0], kvs);
        assertEquals(Map.of("app", "test", "lvl", "INFO", "thread", "thread-1", "class", "t.TestApp"), kvs);

        kvs.clear();
        extractor.extract(Loki4jAppenderTest.events[1], kvs);
        assertEquals(Map.of("app", "test", "lvl", "WARN", "thread", "thread-2", "class", "t.TestApp"), kvs);

        kvs.clear();
        extractor.extract(Loki4jAppenderTest.events[2], kvs);
        assertEquals(Map.of("app", "test", "lvl", "INFO", "thread", "thread-1", "class", "t.TestApp"), kvs);
    }
}
