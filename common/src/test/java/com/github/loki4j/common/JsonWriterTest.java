package com.github.loki4j.common;

import org.junit.Test;

import static com.github.loki4j.common.LogRecord.create;
import static org.junit.Assert.*;

public class JsonWriterTest {
    
    @Test
    public void testWriteRecord() {
        var re1 = create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message");

        var writer = new JsonWriter(1000);
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
        var re1 = create(100L, 1, "level=INFO,app=my-app", "спец !@#$%^&*()\" \n\tсимволы <>?/\\№ё:{}[]🏁");

        var writer = new JsonWriter(1000);
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
