package com.github.loki4j.logback;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import org.junit.Test;

import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.logback.Generators.FailingHttpClient;
import com.github.loki4j.logback.Generators.FailingStringWriter;
import com.github.loki4j.logback.Generators.StoppableHttpClient;
import com.github.loki4j.logback.Generators.WrappingHttpSender;
import com.github.loki4j.testkit.dummy.StringPayload;

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
    public void testDrainOnStopWhileEncoderFails() {
        var failingWriterRef = new AtomicReference<FailingStringWriter>();
        var encoder = wrapToEncoder(
                (c, bf) -> {
                    var failingWriter = new FailingStringWriter(c, bf);
                    failingWriterRef.set(failingWriter);
                    return failingWriter;
                },
                labelCfg("level=%level,app=my-app", ",", "=", true, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                true,
                false);
        var sender = dummySender();
        var appender = appender(4, 4000L, encoder, sender);
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
        assertTrue("no batches before stop", sender.lastBatch() == null);

        try { Thread.sleep(300L); } catch (InterruptedException e1) { }
        assertTrue("no batches before stop", sender.lastBatch() == null);

        failingWriterRef.get().fail.set(false);
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
    public void testRetry() throws InterruptedException, BrokenBarrierException, TimeoutException {
        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<FailingHttpClient>(failingHttpClient);
        var encoder = defaultToStringEncoder();
        var appender = appender(1, 4000L, encoder, sender);
        appender.setPipelineBuilder(PipelineConfig.builder().setSleep((a, b) -> true));
        appender.start();

        sender.client.fail.set(true);
        appender.append(events[0]);

        // all retries failed
        StringPayload expectedPayload = StringPayload.builder()
            .stream("[level, INFO, app, my-app]",
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("send", 1, sender.client.sendCount);
        assertEquals("send", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("retry1", 2, sender.client.sendCount);
        assertEquals("retry1", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("retry2", 3, sender.client.sendCount);
        assertEquals("retry2", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        // first retry is successful
        StringPayload expected2 = StringPayload.builder()
            .stream("[level, WARN, app, my-app]",
                "ts=104 l=WARN c=test.TestApp t=thread-2 | Test message 2 ")
            .build();
        appender.append(events[1]);

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("send-2", 4, sender.client.sendCount);
        assertEquals("send-2", expected2, StringPayload.parse(sender.client.lastBatch, encoder.charset));
        failingHttpClient.await();

        sender.client.fail.set(false);

        failingHttpClient.await();
        assertEquals("retry1-2", 5, sender.client.sendCount);
        assertEquals("retry1-2", expected2, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        appender.stop();
    }

    @Test
    public void testRateLimitedRetry() throws InterruptedException, BrokenBarrierException, TimeoutException {
        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<>(failingHttpClient);
        var encoder = defaultToStringEncoder();

        // retries rate limited requests by default
        var appender = buildRateLimitedAppender(false, encoder, sender);
        appender.setPipelineBuilder(PipelineConfig.builder().setSleep((a, b) -> true));
        appender.start();
        appender.append(events[0]);

        // all retries failed
        StringPayload expectedPayload = StringPayload.builder()
            .stream("[level, INFO, app, my-app]",
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("send", 1, sender.client.sendCount);
        assertEquals("send", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("retry1", 2, sender.client.sendCount);
        assertEquals("retry1", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("retry2", 3, sender.client.sendCount);
        assertEquals("retry2", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        appender.stop();
    }

    @Test
    public void testRateLimitedNoRetries() throws InterruptedException, BrokenBarrierException, TimeoutException {
        var encoder = defaultToStringEncoder();
        var failingHttpClient = new FailingHttpClient();
        var sender = new WrappingHttpSender<>(failingHttpClient);

        // retries rate limited requests
        var appender = buildRateLimitedAppender(true, encoder, sender);
        appender.setDropRateLimitedBatches(true);
        BiFunction<Integer, Long, Boolean> failIfSleep = (i, j) -> {
            throw new IllegalStateException("It should not attempt to retry.");
        };
        appender.setPipelineBuilder(PipelineConfig.builder().setSleep(failIfSleep));
        StringPayload expectedPayload = StringPayload.builder()
            .stream("[level, INFO, app, my-app]",
                "ts=100 l=INFO c=test.TestApp t=thread-1 | Test message 1 ")
            .build();

        appender.start();

        appender.append(events[0]);
        failingHttpClient.await();
        failingHttpClient.await();
        assertEquals("send-2", 1, sender.client.sendCount);
        assertEquals("send-2", expectedPayload, StringPayload.parse(sender.client.lastBatch, encoder.charset));

        appender.stop();
    }

    private Loki4jAppender buildRateLimitedAppender(
            boolean dropRateLimitedBatches,
            AbstractLoki4jEncoder encoder,
            WrappingHttpSender<FailingHttpClient> sender) {
        var appender = appender(1, 4000L, encoder, sender);
        appender.setDropRateLimitedBatches(dropRateLimitedBatches);

        sender.client.fail.set(true);
        sender.client.rateLimited.set(true);

        return appender;
    }

}
