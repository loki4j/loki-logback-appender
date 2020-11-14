package com.github.loki4j.logback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.github.loki4j.common.LogRecord;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class Generators {

    public static Exception exception(String message) {
        return new RuntimeException(message);
    }

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

    public static void withAppender(AbstractLoki4jAppender appender, Consumer<AbstractLoki4jAppender> body) {
        appender.start();
        try {
            body.accept(appender);
        } finally {
            appender.stop();
        }
    }

    public static DummyLoki4jAppender dummyAppender(
        int batchSize,
        long batchTimeoutMs,
        Loki4jEncoder encoder) {
        var appender = new DummyLoki4jAppender();
        appender.setContext(new LoggerContext());
        appender.setBatchSize(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        appender.setEncoder(encoder);
        appender.setVerbose(true);
        return appender;
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

    public static ILoggingEvent loggingEvent(
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

    public static LogRecord logRecord(
            long timestamp,
            int nanos,
            String stream,
            String message) {
        var r = new LogRecord();
        r.timestampMs = timestamp;
        r.nanos = nanos;
        r.stream = stream;
        r.streamHashCode = stream.hashCode();
        r.message = message;
        return r;
    }


    public static class DummyLoki4jAppender extends AbstractLoki4jAppender {
        public List<byte[]> batches = new ArrayList<>();
        public byte[] lastBatch;
        private final ReentrantLock lock = new ReentrantLock(false);

        @Override
        protected void startHttp(String contentType) {}
        @Override
        protected void stopHttp() {}
        @Override
        protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
            lock.lock();
            batches.add(batch);
            lastBatch = batch;
            lock.unlock();
            return CompletableFuture.completedFuture(new LokiResponse(204, ""));
        }
    }


    public static class LokiHttpServerMock {
        public List<byte[]> batches = new ArrayList<>();
        public volatile byte[] lastBatch;

        private final HttpServer server;

        public LokiHttpServerMock(int port) throws IOException {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/loki/api/v1/push", httpExchange -> {
                try (var is = httpExchange.getRequestBody()) {
                    lastBatch = is.readAllBytes();
                    batches.add(lastBatch);
                }
                httpExchange.sendResponseHeaders(204, 0);
            });
        }

        public void start() {
            new Thread(server::start).start();
        }

        public void stop() {
            server.stop(0);
        }

        public void reset() {
            batches.clear();
            lastBatch = null;
        }
    }
}
