package com.github.loki4j.common;

import org.junit.Test;

import static com.github.loki4j.common.LogRecord.create;
import static org.junit.Assert.*;

public class JsonWriterTest {

    private LogRecordStream stream1 = LogRecordStream.create("level", "INFO", "app", "my-app");
    private LogRecordStream stream2 = LogRecordStream.create("level", "DEBUG", "app", "my-app");
    private LogRecordBatch batch = new LogRecordBatch(new LogRecord[] {
        LogRecord.create(3000, stream2, "l=DEBUG c=test.TestApp t=thread-2 | Test message 2"),
        LogRecord.create(1000, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
        LogRecord.create(2000, stream1, "l=INFO c=test.TestApp t=thread-3 | Test message 4"),
        LogRecord.create(5000, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
    });

    private String expectedJson = (
        "{'streams':[{'stream':{'level':'DEBUG','app':'my-app'},'values':" +
        "[['3000000000','l=DEBUG c=test.TestApp t=thread-2 | Test message 2']]}," +
        "{'stream':{'level':'INFO','app':'my-app'},'values':" +
        "[['1000000000','l=INFO c=test.TestApp t=thread-1 | Test message 1']," +
        "['2000000000','l=INFO c=test.TestApp t=thread-3 | Test message 4']," +
        "['5000000000','l=INFO c=test.TestApp t=thread-1 | Test message 3']]}]}"
        ).replace('\'', '"');

    @Test
    public void testWriteBatch() {
        var writer = new JsonWriter(1000);
        assertEquals("initial size is 0", 0, writer.size());

        writer.serializeBatch(batch);
        //assertEquals("size is correct", expectedJson.getBytes().length, writer.size());

        var actualJson = new String(writer.toByteArray());
        assertEquals("encoded json", expectedJson, actualJson);
    }
    
    @Test
    public void testWriteRecord() {
        var re1 = create(
            100L,
            LogRecordStream.create("level", "INFO", "app", "my-app"),
            "l=INFO c=test.TestApp t=thread-1 | Test message");

        var writer = new JsonWriter(1000);
        writer.serializeBatch(new LogRecordBatch(new LogRecord[] {re1}));

        var actual = new String(writer.toByteArray());
        var expected = (
            "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" +
            "[['100000000','l=INFO c=test.TestApp t=thread-1 | Test message']]}]}"
        ).replace('\'', '"');

        assertEquals("single record", expected, actual);
    }

    @Test
    public void testWriteSpecialCharsRecord() {
        var re1 = create(
            100L,
            LogRecordStream.create("level", "INFO", "app", "my-app"),
            "спец !@#$%^&*()\" \n\tсимволы <>?/\\№ё:{}[]🏁");

        var writer = new JsonWriter(1000);
        writer.serializeBatch(new LogRecordBatch(new LogRecord[] {re1}));

        var actual = new String(writer.toByteArray());
        //System.out.println(actual);
        var expected = (
            "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" +
            "[['100000000','спец !@#$%^&*()\\' \\n\\tсимволы <>?/\\\\№ё:{}[]🏁']]}]}"
        ).replace('\'', '"');

        assertEquals("single record", expected, actual);
    }
}
