package com.github.loki4j.logback;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

import com.github.loki4j.client.util.OrderedMap;
import com.github.loki4j.logback.json.AbstractFieldJsonProvider;
import com.github.loki4j.logback.json.JsonEventWriter;
import com.github.loki4j.logback.json.JsonProvider;
import com.github.loki4j.logback.json.LogLevelJsonProvider;
import com.github.loki4j.logback.json.LoggerNameJsonProvider;
import com.github.loki4j.logback.json.MessageJsonProvider;
import com.github.loki4j.logback.json.StackTraceJsonProvider;
import com.github.loki4j.logback.json.ThreadNameJsonProvider;
import com.github.loki4j.logback.json.TimestampJsonProvider;

import com.github.loki4j.testkit.dummy.StringPayload;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonLayoutTest {

    private static final ILoggingEvent[] events = new ILoggingEvent[] {
        loggingEvent(100L, Level.INFO, "io.test.TestApp", "main", "m1-line1\r\nline2\r\n", null),
        loggingEvent(101L, Level.DEBUG, "io.test.TestApp", "thread-1", "m2-line1", null),
        loggingEvent(102L, Level.WARN, "io.test.TestApp", "main", "m3-line1\nline2\r", null)
    };

    @Test
    public void testWorksInAppender() {
        var expected = StringPayload.builder()
            .stream(OrderedMap.of("app", "my-app"),
                "ts=100 {'timestamp_ms':100,'logger_name':'io.test.TestApp','level':'INFO','thread_name':'main','message':'m1-line1\\r\\nline2\\r\\n'}".replace('\'', '"'),
                "ts=101 {'timestamp_ms':101,'logger_name':'io.test.TestApp','level':'DEBUG','thread_name':'thread-1','message':'m2-line1'}".replace('\'', '"'),
                "ts=102 {'timestamp_ms':102,'logger_name':'io.test.TestApp','level':'WARN','thread_name':'main','message':'m3-line1\\nline2\\r'}".replace('\'', '"')
            )
            .build();

        var encoder = stringEncoder(
                labelCfg("app=my-app", ",", "=", true, false),
                jsonMsgLayout(),
                false
        );
        var sender = dummySender();
        withAppender(appender(3, 1000L, encoder, sender), appender -> {
            appender.append(events);
            appender.waitAllAppended();

            var actual = StringPayload.parse(sender.lastSendData());
            assertEquals("jsonLayout", expected, actual);
            return null;
        });
    }

    @Test
    public void testNoEnabledProviders() {
        var layout = jsonMsgLayout();
        
        layout.setLogLevel(disable(LogLevelJsonProvider::new));
        layout.setLoggerName(disable(LoggerNameJsonProvider::new));
        layout.setMessage(disable(MessageJsonProvider::new));
        layout.setThreadName(disable(ThreadNameJsonProvider::new));
        layout.setTimestamp(disable(TimestampJsonProvider::new));
        
        layout.start();

        assertEquals("json empty", "{}", layout.doLayout(events[0]));

        layout.stop();
    }

    @Test
    public void testCustomProvider() {
        var layout = jsonMsgLayout();
        
        layout.setLogLevel(disable(LogLevelJsonProvider::new));
        layout.setLoggerName(disable(LoggerNameJsonProvider::new));
        layout.setThreadName(disable(ThreadNameJsonProvider::new));
        layout.setTimestamp(disable(TimestampJsonProvider::new));

        layout.addCustomProvider(new AbstractFieldJsonProvider() {
            @Override
            protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
                writer.writeNumericField("msg_length", Long.valueOf(event.getMessage().length()));
            }
        });
        
        layout.start();

        assertEquals("custom provider", "{\"message\":\"m2-line1\",\"msg_length\":8}", layout.doLayout(events[1]));

        layout.stop();
    }

    @Test
    public void testThreadSafety() throws Exception {
        class Result {
            final String expected;
            final String actual;

            public Result(final String expected, final String actual) {
                this.expected = expected;
                this.actual = actual;
            }
        }

        final List<Result> results = Collections.synchronizedList(new ArrayList<>());

        final var layout = jsonMsgLayout();
        try {
            layout.setLogLevel(disable(LogLevelJsonProvider::new));
            layout.setLoggerName(disable(LoggerNameJsonProvider::new));
            layout.setTimestamp(disable(TimestampJsonProvider::new));
            layout.setStackTrace(disable(StackTraceJsonProvider::new));

            layout.start();

            // ready...
            final CountDownLatch latch = new CountDownLatch(1);
            final ExecutorService executorService = Executors.newFixedThreadPool(16);
            // set...
            for (final var event : generateEvents(256, 4)) {
                executorService.submit(() -> {
                    awaitLatch(latch);
                    final String formatted = layout.doLayout(event);
                    final String expected = String.format("{'thread_name':'%s','message':'%s'}", event.getThreadName(), event.getMessage())
                            .replace("\n", "\\n")
                            .replace('\'', '"');
                    results.add(new Result(expected, formatted));
                });
            }

            // go!
            latch.countDown();
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10L, TimeUnit.SECONDS));

            for (final Result result : results) {
                assertEquals(result.expected, result.actual);
            }
        } finally {
            layout.stop();
        }
    }

    private static void awaitLatch(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private <T extends JsonProvider<?>> T disable(final Supplier<T> ctor) {
        final var provider = ctor.get();
        provider.setEnabled(false);
        return provider;
    }

}
