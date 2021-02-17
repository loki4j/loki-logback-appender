package com.github.loki4j.logback;

import org.junit.Test;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;

import static com.github.loki4j.logback.Generators.*;

public class JsonEncoderTest {

    private LogRecordBatch records = new LogRecordBatch(new LogRecord[] {
        LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
        LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=DEBUG c=test.TestApp t=thread-2 | Test message 2"),
        LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
        LogRecord.create(102L, 4, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-3 | Test message 4"),
    });

    private static JsonEncoder jsonEncoder(boolean staticLabels) {
        var encoder = new JsonEncoder();
        encoder.setStaticLabels(staticLabels);

        // we don't use these settings in tests
        // they are tested in AbstractLoki4jEncoderTest
        encoder.setLabel(labelCfg("level=%level,app=my-app", ",", "=", true));
        encoder.setMessage(messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"));
        encoder.setSortByTime(false);
        return encoder;
    }

    @Test
    public void testEncodeStaticLabels() {
        withEncoder(jsonEncoder(true), encoder -> {
            var expected = (
                "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" + 
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1']," +
                "['103000002','l=DEBUG c=test.TestApp t=thread-2 | Test message 2']," +
                "['105000003','l=INFO c=test.TestApp t=thread-1 | Test message 3']," +
                "['102000004','l=INFO c=test.TestApp t=thread-3 | Test message 4']]}]}"
                ).replace('\'', '"');
            assertEquals("static labels", expected, new String(encoder.encode(records), encoder.charset));
        });
    }

    @Test
    public void testEncodeDynamicLabels() {
        withEncoder(jsonEncoder(false), encoder -> {
            var expected = (
                "{'streams':[{'stream':{'level':'DEBUG','app':'my-app'},'values':" + 
                "[['103000002','l=DEBUG c=test.TestApp t=thread-2 | Test message 2']]}," +
                "{'stream':{'level':'INFO','app':'my-app'},'values':" + 
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1']," +
                "['105000003','l=INFO c=test.TestApp t=thread-1 | Test message 3']," +
                "['102000004','l=INFO c=test.TestApp t=thread-3 | Test message 4']]}]}"
                ).replace('\'', '"');
            assertEquals("dynamic labels", expected, new String(encoder.encode(records), encoder.charset));
        });
    }

    @Test
    public void testEncodeEscapes() {
        var escRecords = new LogRecordBatch(new LogRecord[] {
            LogRecord.create(100L, 1, "level=INFO,\napp=my-app\r", "l=INFO c=test.TestApp t=thread-1 | Test message 1\nNew line"),
            LogRecord.create(103L, 2, "level=DEBUG,\r\napp=my-app", "l=DEBUG c=test.TestApp t=thread-2\t|\tTest message 2\r\nNew Line")
        });
        withEncoder(jsonEncoder(false), encoder -> {
            var expected = (
                "{'streams':[{'stream':{'level':'DEBUG','\\r\\napp':'my-app'},'values':" +
                "[['103000002','l=DEBUG c=test.TestApp t=thread-2\\t|\\tTest message 2\\r\\nNew Line']]}," +
                "{'stream':{'level':'INFO','\\napp':'my-app\\r'},'values':" +
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1\\nNew line']]}]}"
                ).replace('\'', '"');
            assertEquals("dynamic labels", expected, new String(encoder.encode(escRecords), encoder.charset));
        });
    }
    
}
