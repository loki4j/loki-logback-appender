package com.github.loki4j.logback;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.testkit.dummy.ExceptionGenerator;
import com.github.loki4j.testkit.dummy.LokiHttpServerMock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static com.github.loki4j.testkit.dummy.Generators.genMessage;

public class Generators {

    public static String batchToString(LogRecord[] batch) {
        var s = new StringBuilder();
        for (int i = 0; i < batch.length; i++) {
            s.append(batch[i]);
            s.append('\n');
        }
        return s.toString();
    }

    public static LokiHttpServerMock lokiMock(int port) {
        try {
			return new LokiHttpServerMock(port);
		} catch (IOException e) {
			throw new RuntimeException("Error while creating Loki mock", e);
		}
    }

    public static Loki4jAppender appender(
            int batchSize,
            long batchTimeoutMs,
            Loki4jEncoder encoder,
            AbstractHttpSender sender) {
        var appender = new Loki4jAppender();
        appender.setContext(new LoggerContext());
        appender.setBatchSize(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        appender.setFormat(encoder);
        appender.setHttp(sender);
        appender.setVerbose(true);
        return appender;
    }

    public static JavaHttpSender javaHttpSender(String url) {
        return javaHttpSender(url,null);
    }

    public static JavaHttpSender javaHttpSender(String url,String tenantId) {
        var sender = new JavaHttpSender();

        sender.setUrl(url);
        sender.setTenantId(tenantId);
        sender.setConnectionTimeoutMs(1000L);
        sender.setRequestTimeoutMs(500L);

        return sender;
    }

    public static ApacheHttpSender apacheHttpSender(String url) {
        var sender = new ApacheHttpSender();

        sender.setUrl(url);
        sender.setConnectionTimeoutMs(1000L);
        sender.setRequestTimeoutMs(500L);

        return sender;
    }

    public static DummyHttpSender dummySender() {
        var sender = new DummyHttpSender();
        return sender;
    }

    public static JsonEncoder jsonEncoder(boolean staticLabels, String testLabel) {
        var encoder = new JsonEncoder();
        encoder.setStaticLabels(staticLabels);
        encoder.setLabel(labelCfg("test=" + testLabel + ",level=%level,app=my-app", ",", "=", true));
        encoder.setSortByTime(true);
        return encoder;
    }

    public static ProtobufEncoder protobufEncoder(boolean staticLabels, String testLabel) {
        var encoder = new ProtobufEncoder();
        encoder.setStaticLabels(staticLabels);
        encoder.setLabel(labelCfg("test=" + testLabel + ",level=%level,app=my-app", ",", "=", true));
        return encoder;
    }

    public static <U> U withAppender(
            Loki4jAppender appender,
            Function<AppenderWrapper, U> body) {
        appender.start();
        var wrapper = new AppenderWrapper(appender);
        try {
            return body.apply(wrapper);
        } finally {
            appender.stop();
        }
    }

    public static AbstractLoki4jEncoder defaultToStringEncoder() {
        return toStringEncoder(
            labelCfg("level=%level,app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            true,
            false);
    }

    public static void withEncoder(AbstractLoki4jEncoder encoder, Consumer<AbstractLoki4jEncoder> body) {
        encoder.setContext(new LoggerContext());
        encoder.start();
        try {
            body.accept(encoder);
        } finally {
            encoder.stop();
        }
    }

    public static AbstractLoki4jEncoder toStringEncoder(
        AbstractLoki4jEncoder.LabelCfg label,
        AbstractLoki4jEncoder.MessageCfg message,
        boolean sortByTime,
        boolean staticLabels) {
        var encoder = new AbstractLoki4jEncoder() {
            @Override
            public String getContentType() {
                return "text/plain";
            }
            @Override
            protected byte[] encodeStaticLabels(LogRecord[] batch) {
                return batchToString(batch).getBytes(charset);
            }
            @Override
            protected byte[] encodeDynamicLabels(LogRecord[] batch) {
                return batchToString(batch).getBytes(charset);
            }
        };
        encoder.setLabel(label);
        encoder.setMessage(message);
        encoder.setSortByTime(sortByTime);
        encoder.setStaticLabels(staticLabels);
        return encoder;
    }

    public static AbstractLoki4jEncoder.LabelCfg labelCfg(
            String pattern,
            String pairSeparator,
            String keyValueSeparator,
            boolean nopex) {
        var label = new AbstractLoki4jEncoder.LabelCfg();
        label.setPattern(pattern);
        label.setPairSeparator(pairSeparator);
        label.setKeyValueSeparator(keyValueSeparator);
        label.setNopex(nopex);
        return label;
    }

    public static AbstractLoki4jEncoder.MessageCfg messageCfg(
            String pattern) {
        var message = new AbstractLoki4jEncoder.MessageCfg();
        message.setPattern(pattern);
        return message;
    }

    public static class InfiniteEventIterator implements Iterator<ILoggingEvent> {
        private LoggingEvent[] es;
        private int idx = -1;
        private long timestampMs = 0L;
        public InfiniteEventIterator(LoggingEvent[] events) {
            this.es = events;
            timestampMs = events[0].getTimeStamp();
        }
        @Override
        public boolean hasNext() {
            return true;
        }
        @Override
        public ILoggingEvent next() {
            idx++;
            if (idx >= es.length) {
                timestampMs++;
                idx = 0;
            }
            es[idx].setTimeStamp(timestampMs);
            return es[idx];
        }
        public Iterator<ILoggingEvent> limited(long limit) {
            Iterable<ILoggingEvent> iterable = () -> this;
            return StreamSupport.stream(iterable.spliterator(), false)
                .limit(limit)
                .iterator();
        }
        public static InfiniteEventIterator from(LoggingEvent[] sampleEvents) {
            return new InfiniteEventIterator(sampleEvents);
        }
    }

    public static LoggingEvent[] generateEvents(int maxMessages, int maxWords) {
         var events = new ArrayList<ILoggingEvent>(maxMessages);
         var time = Instant.now().minusMillis(maxMessages).toEpochMilli();

        for (int i = 0; i < maxMessages; i++) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            double lev = rnd.nextDouble();
            String msg = genMessage(maxWords);
            if (lev < 0.7)
                events.add(loggingEvent(
                    time + i,
                    Level.INFO,
                    "test.TestApp",
                    "thread-" + (i % 8),
                    String.format("#%s - %s", i, msg),
                    null));
            else if (lev < 0.8)
            events.add(loggingEvent(
                time + i,
                Level.DEBUG,
                "test.TestApp",
                "thread-" + (i % 8),
                String.format("#%s - %s", i, msg),
                null));
            else if (lev < 0.9)
            events.add(loggingEvent(
                time + i,
                Level.WARN,
                "test.TestApp",
                "thread-" + (i % 8),
                String.format("#%s - %s", i, msg),
                null));
            else
            events.add(loggingEvent(
                time + i,
                Level.ERROR,
                "test.TestApp",
                "thread-" + (i % 8),
                String.format("#%s - %s", i, "Error occurred"),
                ExceptionGenerator.exception(msg)));
        }

        return events.toArray(new LoggingEvent[0]);
    }

    public static LoggingEvent loggingEvent(
            long timestamp,
            Level level,
            String className,
            String threadName,
            String message,
            Throwable throwable) {
        var e = new LoggingEvent();
        e.setTimeStamp(timestamp);
        e.setLevel(level);
        e.setLoggerName(className);
        e.setThreadName(threadName);
        e.setMessage(message);
        if (throwable != null)
            e.setThrowableProxy(new ThrowableProxy(throwable));
        return e;
    }

    public static class AppenderWrapper {
        private Loki4jAppender appender;
        public AppenderWrapper(Loki4jAppender appender) {
            this.appender = appender;
        }
        public void append(ILoggingEvent event) {
            appender.append(event);
        }
        @SuppressWarnings("unchecked")
        public void appendAndWait(ILoggingEvent... events) {
            var fs = (CompletableFuture<Void>[]) new CompletableFuture[events.length];
            for (int i = 0; i < events.length; i++) {
                fs[i] = appender.appendAsync(events[i]);
            }
            try {
                CompletableFuture.allOf(fs).get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Error while waiting for futures", e);
            }
        }
        public void stop() {
            appender.stop();
        }
    }

    public static class DummyHttpSender extends AbstractHttpSender {
        public byte[] lastBatch;
        private final ReentrantLock lock = new ReentrantLock(false);

        @Override
        public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
            lock.lock();
            lastBatch = batch;
            lock.unlock();
            return CompletableFuture.completedFuture(new LokiResponse(204, ""));
        }
    }

}
