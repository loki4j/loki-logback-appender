package com.github.loki4j.logback.json;

import static com.github.loki4j.logback.Generators.loggingEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.qos.logback.classic.Level;

public class MdcJsonProviderTest {
    
    @Test
    public void testNoMdcInEvent() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);

        var provider = new MdcJsonProvider();
        provider.start();

        assertFalse("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "", writer.toString());

        provider.stop();
    }

    @Test
    public void testOneMdcInEvent() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.getMDCPropertyMap().put("property1", "value1");

        var provider = new MdcJsonProvider();
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"mdc_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testExclude() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.getMDCPropertyMap().put("property1", "value1");
        event.getMDCPropertyMap().put("property2", "value2");

        var provider = new MdcJsonProvider();
        provider.addExclude("property2");
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"mdc_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testInclude() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.getMDCPropertyMap().put("property1", "value1");
        event.getMDCPropertyMap().put("property2", "value2");

        var provider = new MdcJsonProvider();
        provider.addInclude("property1");
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"mdc_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testOmitPrefix() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.getMDCPropertyMap().put("property1", "value1");

        var provider = new MdcJsonProvider();
        provider.setNoPrefix(true);
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"property1\":\"value1\"", writer.toString());

        provider.stop();
    }
}
