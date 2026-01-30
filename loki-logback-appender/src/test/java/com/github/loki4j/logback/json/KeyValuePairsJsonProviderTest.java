package com.github.loki4j.logback.json;

import static com.github.loki4j.logback.Generators.loggingEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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
    public void testOneKvpInEvent() {
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
        provider.setPrefix("key_value_");
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"key_value_property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testOmitPrefix() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("property1", "value1")
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.setNoPrefix(true);
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo", "\"property1\":\"value1\"", writer.toString());

        provider.stop();
    }

    @Test
    public void testCustomFieldSerializer() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("property1", "value1"),
                new KeyValuePair("property2", "value2")
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.setFieldSerializer(
                (writer, name, value) -> {
                    writer.writeArrayField(name + "_arr", new String[] { name, value.toString() });
                }
        );
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo",
                "\"kvp_property1_arr\":[\"kvp_property1\",\"value1\"],\"kvp_property2_arr\":[\"kvp_property2\",\"value2\"]",
                writer.toString()
        );

        provider.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomIterableSerializer() {
        var event = loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("prop", List.of(new TestData("Aaaa", 10), new TestData("Bbbb", 20)))
        ));

        var provider = new KeyValuePairsJsonProvider();
        provider.setFieldSerializer(
                (writer, name, value) -> {
                    writer.writeArrayField(name, (Iterable<TestData>)value, (w, o) -> {
                        w.writeBeginObject();
                        w.writeStringField("name", o.name);
                        w.writeFieldSeparator();
                        w.writeNumericField("age", o.age);
                        w.writeEndObject();
                    });
                }
        );
        provider.start();

        assertTrue("canWrite", provider.canWrite(event));

        var writer = new JsonEventWriter(0);
        provider.writeTo(writer, event, false);

        assertEquals("writeTo",
                "\"kvp_prop\":[{\"name\":\"Aaaa\",\"age\":10},{\"name\":\"Bbbb\",\"age\":20}]",
                writer.toString()
        );

        provider.stop();
    }

    private class TestData {
        public String name;
        public int age;
        public TestData(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}
