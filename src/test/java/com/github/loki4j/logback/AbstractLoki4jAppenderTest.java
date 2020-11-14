package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jAppenderTest {

    static ILoggingEvent[] events = new ILoggingEvent[] {
        loggingEvent(
            100L,
            Level.INFO,
            "test.TestApp",
            "thread-1",
            "Test message 1",
            null),
        loggingEvent(
            104L,
            Level.WARN,
            "test.TestApp",
            "thread-2",
            "Test message 2",
            null),
        loggingEvent(
            107L,
            Level.INFO,
            "test.TestApp",
            "thread-1",
            "Test message 3",
            null)
    };

    static String expected =
            "LogRecord [ts=100, nanos=1, stream=level=INFO,app=my-app, message=l=INFO c=test.TestApp t=thread-1 | Test message 1 ]\n" +
            "LogRecord [ts=107, nanos=3, stream=level=INFO,app=my-app, message=l=INFO c=test.TestApp t=thread-1 | Test message 3 ]\n" +
            "LogRecord [ts=104, nanos=2, stream=level=WARN,app=my-app, message=l=WARN c=test.TestApp t=thread-2 | Test message 2 ]\n";

    @Test
    public void testBatchSize() {
        var encoder = defaultToStringEncoder();
        var appender = dummyAppender(3, 1000L, encoder);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        assertTrue("no batches before batchSize reached", appender.lastBatch == null);

        appender.append(events[2]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        assertEquals("batchSize", expected, new String(appender.lastBatch, encoder.charset));
        appender.stop();
    }

    @Test
    public void testBatchTimeout() {
        var encoder = defaultToStringEncoder();
        var appender = dummyAppender(30, 400L, encoder);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before batchTimeout reached", appender.lastBatch == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before batchTimeout reached", appender.lastBatch == null);
        
        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertEquals("batchTimeout", expected, new String(appender.lastBatch, encoder.charset));
        appender.stop();
    }

    @Test
    public void testDrainOnStop() {
        var encoder = defaultToStringEncoder();
        var appender = dummyAppender(30, 4000L, encoder);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", appender.lastBatch == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", appender.lastBatch == null);
        
        appender.stop();
        assertEquals("batchTimeout", expected, new String(appender.lastBatch, encoder.charset));
    }
}
