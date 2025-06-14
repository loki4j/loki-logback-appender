package com.github.loki4j.logback;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.util.OrderedMap;
import com.github.loki4j.logback.Generators.WrappingHttpSender;
import com.github.loki4j.testkit.dummy.FailingHttpClient;
import com.github.loki4j.testkit.dummy.FailingStringWriter;
import com.github.loki4j.testkit.dummy.StringPayload;
import com.github.loki4j.testkit.dummy.SuspendableHttpClient;
import com.github.loki4j.testkit.dummy.FailingHttpClient.FailureType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

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
        .stream(OrderedMap.of("level", "INFO", "app", "my-app"),
            "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ",
            "ts=107 l=INFO c=test.TestApp t=thread-1 | Test message 3 ")
        .stream(OrderedMap.of("level", "WARN", "app", "my-app"),
            "ts=104 l=WARN c=test.TestApp t=thread-2 | Test message 2 ")
        .build();

    @Test
    public void testBatchSize() {
        var sender = dummySender();
        withAppender(appender(batch(3, 1000L), http(sender)), appender -> {
            var sendCapture = sender.captureSendInvocation();
            appender.append(events[0]);
            appender.append(events[1]);
            assertTrue("no batches before batchSize reached", sender.lastSendData() == null);

            appender.append(events[2]);
            var send = sendCapture.waitForNextSend(100);
            assertEquals("batchSize", expected, StringPayload.parse(send.data));
            return null;
        });
    }

    @Test
    public void testBatchTimeout() {
        var sender = dummySender();
        withAppender(appender(batch(30, 400L), http(sender)), appender -> {
            appender.append(events[0]);
            appender.append(events[1]);
            appender.append(events[2]);
            assertTrue("no batches before batchTimeout reached", sender.lastSendData() == null);

            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertTrue("no batches before batchTimeout reached", sender.lastSendData() == null);

            try { Thread.sleep(300L); } catch (InterruptedException e1) { }
            assertEquals("batchTimeout", expected, StringPayload.parse(sender.lastSendData()));
            return null;
        });
    }

    @Test
    public void testDrainOnStop() {
        var sender = dummySender();
        var appender = appender(batch(30, 4000L), http(sender));
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", sender.lastSendData() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastSendData() == null);

        appender.stop();
        assertEquals("batchTimeout", expected, StringPayload.parse(sender.lastSendData()));
    }

    @Test
    public void testDrainOnStopWhileEncoderFails() {
        var failingWriterRef = new AtomicReference<FailingStringWriter>();
        var sender = dummySender();
        var appender = appender(
            "level=%level\napp=my-app",
            Loki4jAppender.DISABLE_SMD_PATTERN,
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            batch(4, 4000L),
            http(
                new PipelineConfig.WriterFactory(
                    (c, bf) -> {
                        var failingWriter = new FailingStringWriter(c, bf);
                        failingWriterRef.set(failingWriter);
                        return failingWriter;
                    },
                    "text/plain"
                ),
                sender)
        );
        appender.start();
        failingWriterRef.get().fail.set(true);
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        appender.append(events[0]);
        try { Thread.sleep(300L); } catch (InterruptedException e1) { }

        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", sender.lastSendData() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastSendData() == null);

        failingWriterRef.get().fail.set(false);
        appender.stop();
        assertEquals("batchTimeout", expected, StringPayload.parse(sender.lastSendData()));
    }

    @Test
    public void testDrainOnStopDisabled() {
        var sender = dummySender();
        var appender = appender(batch(30, 4000L), http(sender));
        appender.getBatch().setDrainOnStop(false);
        appender.start();
        appender.append(events[0]);
        appender.append(events[1]);
        appender.append(events[2]);
        assertTrue("no batches before stop", sender.lastSendData() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastSendData() == null);

        appender.stop();
        assertTrue("no batches after stop", sender.lastSendData() == null);
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

        var sender = dummySender();
        var appender = appender(batch(3, 4000L), http(sender));
        appender.getBatch().setMaxBytes(500);
        appender.start();
        var sendCapture = sender.captureSendInvocation();
        appender.append(events[0]);
        appender.append(loggingEvent(100L, Level.INFO, "TestApp", "main", longStr, null));
        appender.append(events[1]);
        appender.append(events[2]);

        var send = sendCapture.waitForNextSend(100);
        assertEquals("batchSize", expected, StringPayload.parse(send.data));

        appender.stop();
    }

    @Test
    public void testBackpressure() {
        var sender = new WrappingHttpSender<>(new SuspendableHttpClient());
        var appender = appender(batch(1, 4000L), http(sender));
        appender.getBatch().setMaxBytes(120);
        appender.getBatch().setSendQueueMaxBytes(150);
        appender.start();

        sender.client.suspend();
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

        sender.client.resume();
        try { Thread.sleep(100L); } catch (InterruptedException e1) { }

        assertEquals("some events dropped", 3, appender.droppedEventsCount());

        appender.stop();
    }

    @Test
    public void testConnectionExceptionRetry() throws InterruptedException, BrokenBarrierException, TimeoutException, ExecutionException {
        StringPayload expectedPayload = StringPayload.builder()
            .stream(OrderedMap.of("level", "INFO", "app", "my-app"),
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();
        StringPayload expectedPayload2 = StringPayload.builder()
            .stream(OrderedMap.of("level", "WARN", "app", "my-app"),
                "ts=104 l=WARN c=test.TestApp t=thread-2 | Test message 2 ")
            .build();

        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<>(failingHttpClient);

        var appender = appender(batch(1, 4000L), http(sender));
        appender.getHttp().setMinRetryBackoffMs(50);
        appender.getHttp().setMaxRetryJitterMs(10);
        appender.start();

        // all retries failed

        sender.client.setFailure(FailureType.CONNECTION_EXCEPTION);

        var sendCapture = sender.client.captureSendInvocation();
        appender.append(events[0]);

        var send1 =  sendCapture.waitForNextSend(100);
        assertEquals("send", 1, send1.sendNo);
        assertEquals("send", expectedPayload, StringPayload.parse(send1.data));

        var send2 = send1.waitForNextSend(100);
        assertEquals("retry1", 2, send2.sendNo);
        assertEquals("retry1", expectedPayload, StringPayload.parse(send2.data));

        var send3 = send2.waitForNextSend(150);
        assertEquals("retry2", 3, send3.sendNo);
        assertEquals("retry2", expectedPayload, StringPayload.parse(send3.data));

        // first retry is successful

        appender.append(events[1]);

        var send4 =  send3.waitForNextSend(100);
        assertEquals("send-2", 4, send4.sendNo);
        assertEquals("send-2", expectedPayload2, StringPayload.parse(send4.data));

        sender.client.setFailure(FailureType.NONE);

        var send5 = send4.waitForNextSend(100);
        assertEquals("retry1-2", 5, send5.sendNo);
        assertEquals("retry1-2", expectedPayload2, StringPayload.parse(send5.data));

        appender.stop();
    }

    @Test
    public void testHttpTimeoutRetry() throws InterruptedException, BrokenBarrierException, TimeoutException, ExecutionException {
        StringPayload expectedPayload = StringPayload.builder()
            .stream(OrderedMap.of("level", "INFO", "app", "my-app"),
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();

        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<>(failingHttpClient);

        var appender = appender(batch(1, 4000L), http(sender));
        appender.getHttp().setMinRetryBackoffMs(50);
        appender.getHttp().setMaxRetryJitterMs(10);
        appender.start();

        sender.client.setFailure(FailureType.HTTP_CONNECT_TIMEOUT_EXCEPTION);
        var sendCapture = sender.client.captureSendInvocation();

        appender.append(events[0]);

        var send1 =  sendCapture.waitForNextSend(100);
        assertEquals("send", 1, send1.sendNo);
        assertEquals("send", expectedPayload, StringPayload.parse(send1.data));

        var send2 = send1.waitForNextSend(150);
        assertEquals("retry1", 2, send2.sendNo);
        assertEquals("retry1", expectedPayload, StringPayload.parse(send2.data));

        var send3 = send2.waitForNextSend(200);
        assertEquals("retry2", 3, send3.sendNo);
        assertEquals("retry2", expectedPayload, StringPayload.parse(send3.data));

        appender.stop();
    }

    @Test
    public void testRateLimitedRetry() throws InterruptedException, BrokenBarrierException, TimeoutException, ExecutionException {
        StringPayload expectedPayload = StringPayload.builder()
            .stream(OrderedMap.of("level", "INFO", "app", "my-app"),
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();

        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<>(failingHttpClient);

        var appender = appender(batch(1, 4000L), http(sender));
        appender.getHttp().setMinRetryBackoffMs(50);
        appender.getHttp().setMaxRetryJitterMs(10);
        appender.start();

        sender.client.setFailure(FailureType.RATE_LIMITED);
        var sendCapture = sender.client.captureSendInvocation();

        appender.append(events[0]);

        var send1 =  sendCapture.waitForNextSend(100);
        assertEquals("send", 1, send1.sendNo);
        assertEquals("send", expectedPayload, StringPayload.parse(send1.data));

        var send2 = send1.waitForNextSend(150);
        assertEquals("retry1", 2, send2.sendNo);
        assertEquals("retry1", expectedPayload, StringPayload.parse(send2.data));

        var send3 = send2.waitForNextSend(200);
        assertEquals("retry2", 3, send3.sendNo);
        assertEquals("retry2", expectedPayload, StringPayload.parse(send3.data));

        appender.stop();
    }

}
