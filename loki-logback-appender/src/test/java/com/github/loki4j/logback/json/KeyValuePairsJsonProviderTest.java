package com.github.loki4j.logback.json;

import static com.github.loki4j.logback.Generators.loggingEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.Level;

public class KeyValuePairsJsonProviderTest {
    
    @Test
    public void testNoKvpInEvent() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);

        var provider = new KeyValuePairsJsonProvider();
        provider.start();

        assertFalse("canWrite", provider.canWrite(event));

        provider.stop();
    }

    @Test
    public void testOneMdcInEvent() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("property1", "value1")
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"kvp_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testExclude() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("property1", "value1"),
                new KeyValuePair("property2", 12345)
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.addExclude("property2");
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"kvp_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testInclude() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("property1", "value1"),
                new KeyValuePair("property2", 12345)
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.addInclude("property1");
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"kvp_property1\":\"value1\"", writer.toString());

        provider.stop();
    }
}
