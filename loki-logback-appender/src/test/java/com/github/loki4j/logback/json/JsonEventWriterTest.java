package com.github.loki4j.logback.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonEventWriterTest {
    
    @Test
    public void testWriteTypedFields() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeStringField("str", "abc");
        writer.writeFieldSeparator();
        writer.writeNumericField("num", 123);
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

    @Test
    public void testWriteSimpleObjectFields() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeObjectField("str", "abc");
        writer.writeFieldSeparator();
        writer.writeObjectField("num", 123);
        writer.writeFieldSeparator();
        writer.writeObjectField("bul", false);
        writer.writeEndObject();

        assertEquals("{\"str\":\"abc\",\"num\":123,\"bul\":false}", writer.toString());
    }

    @Test
    public void testWriteNullObject() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeObjectField("nul", null);
        writer.writeEndObject();

        assertEquals("{\"nul\":null}", writer.toString());
    }

    @Test
    public void testWriteToStringObject() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeObjectField("dub", 0.987);
        writer.writeEndObject();

        assertEquals("{\"dub\":\"0.987\"}", writer.toString());
    }

    @Test
    public void testWriteRawJsonStringObject() {
        var writer = new JsonEventWriter(10);
        writer.writeBeginObject();
        writer.writeObjectField("raw", RawJsonString.from("{\"arr\":[1,2,3],\"val\":null}"));
        writer.writeEndObject();

        assertEquals("{\"raw\":{\"arr\":[1,2,3],\"val\":null}}", writer.toString());
    }
}
