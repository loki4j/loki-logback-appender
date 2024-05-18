package com.github.loki4j.logback;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class PatternLayoutTest {

    @Test
    public void testEncodeEscapes() {
        ILoggingEvent[] escEvents = new ILoggingEvent[] {
            loggingEvent(100L, Level.INFO, "TestApp", "main", "m1-line1\r\nline2\r\n", null),
            loggingEvent(100L, Level.INFO, "TestApp", "main", "m2-line1\nline2\n", null),
            loggingEvent(100L, Level.INFO, "TestApp", "main", "m3-line1\rline2\r", null)
        };

        var encoder = jsonEncoder(false, "testEncodeEscapes");
        var sender = dummySender();
        var appender = appender(3, 1000L, encoder, sender);
        appender.start();

        appender.append(escEvents[0]);
        appender.append(escEvents[1]);
        appender.append(escEvents[2]);

        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        var expected = (
            "{'streams':[{'stream':{'test':'testEncodeEscapes','level':'INFO','service_name':'my-app'}," +
            "'values':[['100000000','l=INFO c=TestApp t=main | m1-line1\\r\\nline2\\r\\n ']," +
            "['100000000','l=INFO c=TestApp t=main | m2-line1\\nline2\\n ']," +
            "['100000000','l=INFO c=TestApp t=main | m3-line1\\rline2\\r ']]}]}"
            ).replace('\'', '"');

        var actual = new String(sender.lastSendData(), encoder.charset);
        //System.out.println(expected);
        //System.out.println(actual);
        assertEquals("escape", expected, actual);
        appender.stop();
    }

}
