package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.http.LokiResponse;

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

    static StringPayload expected = StringPayload.builder()
        .stream("[level, INFO, app, my-app]",
            "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ",
            "ts=107 l=INFO c=test.TestApp t=thread-1 | Test message 3 ")
        .stream("[level, WARN, app, my-app]",
            "ts=104 l=WARN c=test.TestApp t=thread-2 | Test message 2 ")
        .build();

    @Test
    public void testBatchSize() {
        var encoder = defaultToStringEncoder();
        var sender = dummySender();
        withAppender(appender(3, 1000L, encoder, sender), appender -> {
            appender.append(events[0]);
            appender.append(events[1]);
            assertTrue("no batches before batchSize reached", sender.lastBatch() == null);

            appender.append(events[2]);
            appender.waitAllAppended();
            assertEquals("batchSize", expected, StringPayload.parse(sender.lastBatch(), encoder.charset));
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
            assertTrue("no batches before batchTimeout reached", sender.lastBatch() == null);

            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertTrue("no batches before batchTimeout reached", sender.lastBatch() == null);
        
            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertEquals("batchTimeout", expected, StringPayload.parse(sender.lastBatch(), encoder.charset));
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
        assertTrue("no batches before stop", sender.lastBatch() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastBatch() == null);
        
        appender.stop();
        assertEquals("batchTimeout", expected, StringPayload.parse(sender.lastBatch(), encoder.charset));
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
        assertTrue("no batches before stop", sender.lastBatch() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastBatch() == null);

        appender.stop();
        assertTrue("no batches after stop", sender.lastBatch() == null);
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

        var actual = new String(sender.lastBatch(), encoder.charset);
        System.out.println(expected);
        System.out.println(actual);
        assertEquals("batchSize", expected, actual);
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
        assertEquals("batchSize", expected, StringPayload.parse(sender.lastBatch(), encoder.charset));

        appender.stop();
    }

    @Test
    public void testBackpressure() {
        var sender = new WrappingHttpSender<StoppableHttpClient>(new StoppableHttpClient());
        var encoder = defaultToStringEncoder();
        var appender = appender(1, 4000L, encoder, sender);
        appender.setBatchMaxBytes(120);
        appender.setSendQueueMaxBytes(150);
        appender.start();

        sender.client.wait.set(true);
        // hanging sender
        appender.append(events[0]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        // batcher buffer
        appender.append(events[2]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        // sender queue
        for (int i = 0; i < 6; i++) {
            appender.append(events[0]);
            try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        }

        appender.append(events[0]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        appender.append(events[1]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        appender.append(events[2]);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        sender.client.wait.set(false);
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        assertEquals("some events dropped", 3, appender.droppedEventsCount());

        appender.stop();
    }

    @Test
    public void testRetry() {
        var sender = new WrappingHttpSender<FailingHttpClient>(new FailingHttpClient());
        var encoder = defaultToStringEncoder();
        var appender = appender(1, 4000L, encoder, sender);
        appender.setRetryTimeoutMs(200);
        appender.start();

        // all retries failed
        StringPayload expected = StringPayload.builder()
            .stream("[level, INFO, app, my-app]",
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();
        sender.client.fail.set(true);
        appender.append(events[0]);

        try { Thread.sleep(100L); } catch (InterruptedException e1) { }
        assertEquals("send", 1, sender.client.sendCount);
        assertEquals("send", expected, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        try { Thread.sleep(200L); } catch (InterruptedException e1) { }
        assertEquals("retry1", 2, sender.client.sendCount);
        assertEquals("retry1", expected, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        try { Thread.sleep(200L); } catch (InterruptedException e1) { }
        assertEquals("retry2", 3, sender.client.sendCount);
        assertEquals("retry2", expected, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        // first retry is successful
        StringPayload expected2 = StringPayload.builder()
            .stream("[level, WARN, app, my-app]",
                "ts=104 l=WARN c=test.TestApp t=thread-2 | Test message 2 ")
            .build();
        appender.append(events[1]);

        try { Thread.sleep(50L); } catch (InterruptedException e1) { }
        assertEquals("send-2", 4, sender.client.sendCount);
        assertEquals("send-2", expected2, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        sender.client.fail.set(false);
        try { Thread.sleep(500L); } catch (InterruptedException e1) { }
        assertEquals("retry1-2", 5, sender.client.sendCount);
        assertEquals("retry1-2", expected2, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        appender.stop();
    }

    private static class StoppableHttpClient implements Loki4jHttpClient {
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

        @Override
        public void close() throws Exception {
            lastBatch = null;
        }

        @Override
        public HttpConfig getConfig() {
            return defaultHttpConfig.build("test");
        }
    }

    private static class FailingHttpClient implements Loki4jHttpClient {
        public AtomicBoolean fail = new AtomicBoolean(false);
        public volatile int sendCount = 0;
        public byte[] lastBatch;

        @Override
        public LokiResponse send(ByteBuffer batch) throws ConnectException {
            sendCount++;
            lastBatch = new byte[batch.remaining()];
            batch.get(lastBatch);
            if (fail.get())
                throw new ConnectException("Text exception");
            return new LokiResponse(204, "");
        }

        @Override
        public void close() throws Exception {
            lastBatch = null;
        }

        @Override
        public HttpConfig getConfig() {
            return defaultHttpConfig.build("test");
        }
    }

    private static class WrappingHttpSender<T extends Loki4jHttpClient> extends AbstractHttpSender {
        public final T client;

        public WrappingHttpSender(T client) {
            this.client = client;
        }

        @Override
        public HttpConfig.Builder getConfig() {
            return defaultHttpConfig;
        }

        @Override
        public Function<HttpConfig, Loki4jHttpClient> getHttpClientFactory() {
            return cfg -> client;
        }
    }
}
