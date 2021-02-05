package com.github.loki4j.common;

import org.junit.Test;

import static org.junit.Assert.*;

public class JsonWriterTest {

    private LogRecord logRecord(long ts, int ns, String stream, String msg, JsonWriter writer) {
        writer.record(ts, ns, msg);
        return LogRecord.create(ts, ns, stream, writer.toByteArray());
    }
    
    @Test
    public void testWriteRecord() {
        var writer = new JsonWriter();

        var re1 = logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message", writer);
        writer.beginStreams(re1, new String[] { "level","INFO","app","my-app" });
        writer.endStreams();

        var actual = new String(writer.toByteArray());
        var expected = (
            "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" +
            "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message']]}]}"
        ).replace('\'', '"');

        assertEquals("single record", expected, actual);
    }

    @Test
    public void testWriteSpecialCharsRecord() {
        var writer = new JsonWriter();

        var re1 = logRecord(100L, 1, "level=INFO,app=my-app", "спец !@#$%^&*()\" \n\tсимволы <>?/\\№ё:{}[]🏁", writer);
        writer.beginStreams(re1, new String[] { "level","INFO","app","my-app" });
        writer.endStreams();

        var actual = new String(writer.toByteArray());
        //System.out.println(actual);
        var expected = (
            "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" +
            "[['100000001','спец !@#$%^&*()\\' \\n\\tсимволы <>?/\\\\№ё:{}[]🏁']]}]}"
        ).replace('\'', '"');

        assertEquals("single record", expected, actual);
    }
}
