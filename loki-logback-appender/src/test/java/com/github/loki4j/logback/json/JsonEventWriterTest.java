package com.github.loki4j.logback.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonEventWriterTest {
    
    @Test
    public void testWriteMultipleFields() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeStringField("str", "abc");
        writer.writeFieldSeparator();
        writer.writeNumberField("num", 123);
        writer.writeEndObject();

        assertEquals("{\"str\":\"abc\",\"num\":123}", writer.toString());
    }

    @Test
    public void testWriteNullString() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeStringField("str", null);
        writer.writeEndObject();

        assertEquals("{\"str\":null}", writer.toString());
    }
}
