package com.github.loki4j.logback;

import org.junit.Test;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;

import static com.github.loki4j.logback.Generators.*;

public class JsonLayoutTest {

    private LogRecord[] records = new LogRecord[] {
        LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
        LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=DEBUG c=test.TestApp t=thread-2 | Test message 2"),
        LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
        LogRecord.create(102L, 4, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-3 | Test message 4"),
    };

    private static JsonLayout jsonLayout(boolean staticLabels) {
        var layout = new JsonLayout();
        layout.setStaticLabels(staticLabels);

        // we don't use these settings in tests
        // they are tested in AbstractLoki4jLayoutTest
        layout.setLabel(labelCfg("level=%level,app=my-app", ",", "=", true));
        layout.setMessage(messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"));
        layout.setSortByTime(false);
        return layout;
    }

    @Test
    public void testEncodeStaticLabels() {
        withLayout(jsonLayout(true), layout -> {
            var expected = (
                "{'streams':[{'stream':{'level':'INFO','app':'my-app'},'values':" + 
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1']," +
                "['103000002','l=DEBUG c=test.TestApp t=thread-2 | Test message 2']," +
                "['105000003','l=INFO c=test.TestApp t=thread-1 | Test message 3']," +
                "['102000004','l=INFO c=test.TestApp t=thread-3 | Test message 4']]}]}"
                ).replace('\'', '"');
            assertEquals("static labels", expected, new String(layout.encode(records), layout.charset));
        });
    }

    @Test
    public void testEncodeDynamicLabels() {
        withLayout(jsonLayout(false), layout -> {
            var expected = (
                "{'streams':[{'stream':{'level':'DEBUG','app':'my-app'},'values':" + 
                "[['103000002','l=DEBUG c=test.TestApp t=thread-2 | Test message 2']]}," +
                "{'stream':{'level':'INFO','app':'my-app'},'values':" + 
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1']," +
                "['105000003','l=INFO c=test.TestApp t=thread-1 | Test message 3']," +
                "['102000004','l=INFO c=test.TestApp t=thread-3 | Test message 4']]}]}"
                ).replace('\'', '"');
            assertEquals("dynamic labels", expected, new String(layout.encode(records), layout.charset));
        });
    }

    @Test
    public void testEncodeEscapes() {
        LogRecord[] escRecords = new LogRecord[] {
            LogRecord.create(100L, 1, "level=INFO,\napp=my-app\r", "l=INFO c=test.TestApp t=thread-1 | Test message 1\nNew line"),
            LogRecord.create(103L, 2, "level=DEBUG,\r\napp=my-app", "l=DEBUG c=test.TestApp t=thread-2\t|\tTest message 2\r\nNew Line")
        };
        withLayout(jsonLayout(false), layout -> {
            var expected = (
                "{'streams':[{'stream':{'level':'DEBUG','\\r\\napp':'my-app'},'values':" +
                "[['103000002','l=DEBUG c=test.TestApp t=thread-2\\t|\\tTest message 2\\r\\nNew Line']]}," +
                "{'stream':{'level':'INFO','\\napp':'my-app\\r'},'values':" +
                "[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1\\nNew line']]}]}"
                ).replace('\'', '"');
            assertEquals("dynamic labels", expected, new String(layout.encode(escRecords), layout.charset));
        });
    }
    
}
