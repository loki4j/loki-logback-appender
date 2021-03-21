package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import com.github.loki4j.common.LokiResponse;

import static com.github.loki4j.logback.Generators.*;

public class Loki4jAppenderTest {

    public static ILoggingEvent[] events = new ILoggingEvent[] {
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
            "LogRecord [ts=100, stream=Stream [id=0, labels=[level, INFO, app, my-app]], message=l=INFO c=test.TestApp t=thread-1 | Test message 1 ]\n" +
            "LogRecord [ts=107, stream=Stream [id=0, labels=[level, INFO, app, my-app]], message=l=INFO c=test.TestApp t=thread-1 | Test message 3 ]\n" +
            "LogRecord [ts=104, stream=Stream [id=1, labels=[level, WARN, app, my-app]], message=l=WARN c=test.TestApp t=thread-2 | Test message 2 ]\n";

    @Test
    public void testBatchSize() {
        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        withAppender(appender(3, 1000L, encoder, sender), appender -> {
            appender.append(events[0]);
            appender.append(events[1]);
            assertTrue("no batches before batchSize reached", sender.lastBatch == null);

            appender.append(events[2]);
            appender.waitAllAppended();
            assertEquals("batchSize", expected, new String(sender.lastBatch, encoder.charset));
            return null;
        });
    }

    @Test
    public void testBatchTimeout() {
        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        withAppender(appender(30, 400L, encoder, sender), appender -> {
            appender.append(events[0]);
            appender.append(events[1]);
            appender.append(events[2]);
            assertTrue("no batches before batchTimeout reached", sender.lastBatch == null);

            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertTrue("no batches before batchTimeout reached", sender.lastBatch == null);
        
            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertEquals("batchTimeout", expected, new String(sender.lastBatch, encoder.charset));
            return null;
        });
    }

    @Test
    public void testDrainOnStop() {
        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        var appender = appender(30, 4000L, encoder, sender);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", sender.lastBatch == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastBatch == null);
        
        appender.stop();
        assertEquals("batchTimeout", expected, new String(sender.lastBatch, encoder.charset));
    }

    @Test
    public void testDrainOnStopDisabled() {
        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        var appender = appender(30, 4000L, encoder, sender);
        appender.setDrainOnStop(false);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", sender.lastBatch == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastBatch == null);

        appender.stop();
        assertTrue("no batches after stop", sender.lastBatch == null);
    }

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
            "{'streams':[{'stream':{'test':'testEncodeEscapes','level':'INFO','app':'my-app'}," +
            "'values':[['100100000','l=INFO c=TestApp t=main | m1-line1\\r\\nline2\\r\\n ']," +
            "['100100001','l=INFO c=TestApp t=main | m2-line1\\nline2\\n ']," +
            "['100100002','l=INFO c=TestApp t=main | m3-line1\\rline2\\r ']]}]}"
            ).replace('\'', '"');

        assertEquals("batchSize", expected, new String(sender.lastBatch, encoder.charset));
        appender.stop();
    }

    @Test
    public void testTooLargeEventDropped() {
        var longStr =
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";

        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        var appender = appender(3, 4000L, encoder, sender);
        appender.setBatchMaxBytes(500);
        appender.start();
        appender.append(events[0]);
        appender.append(loggingEvent(100L, Level.INFO, "TestApp", "main", longStr, null));
        appender.append(events[1]);
        appender.append(events[2]);

        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        assertEquals("batchSize", expected, new String(sender.lastBatch, encoder.charset));

        appender.stop();
    }

    @Test
    public void testBackpressure() {
        var sender = new StoppableSender();
        var encoder = defaultToStringEncoder();
        var appender = appender(1, 4000L, encoder, sender);
        appender.setBatchMaxBytes(120);
        appender.setSendQueueMaxBytes(150);
        appender.start();

        sender.wait.set(true);
        // hanging sender
        appender.append(events[0]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        // batcher buffer
        appender.append(events[2]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        // sender queue
        for (int i = 0; i < 4; i++) {
            appender.append(events[0]);
            try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        }

        appender.append(events[0]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        appender.append(events[1]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        appender.append(events[2]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        sender.wait.set(false);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        assertEquals("some events dropped", 3, appender.droppedEventsCount());

        appender.stop();
    }

    private static class StoppableSender extends AbstractHttpSender {
        public AtomicBoolean wait = new AtomicBoolean(false);
        public byte[] lastBatch;

        @Override
        public LokiResponse send(ByteBuffer batch) {
            while(wait.get())
                LockSupport.parkNanos(1000);
            lastBatch = new byte[batch.remaining()];
            batch.get(lastBatch);
            return new LokiResponse(204, "");
        }
    }
}
