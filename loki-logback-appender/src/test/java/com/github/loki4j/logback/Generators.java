package com.github.loki4j.logback;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.slf4j.Marker;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.writer.Writer;
import com.github.loki4j.testkit.dummy.DummyHttpClient;
import com.github.loki4j.testkit.dummy.ExceptionGenerator;
import com.github.loki4j.testkit.dummy.LokiHttpServerMock;
import com.github.loki4j.testkit.dummy.StringWriter;
import com.github.loki4j.testkit.dummy.DummyHttpClient.SendInvocation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Layout;

import static com.github.loki4j.testkit.dummy.Generators.genMessage;


public class Generators {

    static HttpConfig.Builder defaultHttpConfig = HttpConfig.builder();

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
            Consumer<Loki4jAppender> encoder,   // for compatibility with tests using old "encoder" settings
            AbstractHttpSender sender) {
        var appender = new Loki4jAppender();
        appender.setContext(new LoggerContext());
        appender.setBatchMaxItems(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        encoder.accept(appender);
        appender.setHttp(sender);
        appender.setVerbose(true);
        return appender;
    }

    public static JavaHttpSender javaHttpSender(String url) {
        var sender = new JavaHttpSender();

        sender.setUrl(url);
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
        return new DummyHttpSender();
    }

    public static Consumer<Loki4jAppender> jsonEncoder(boolean staticLabels, String testLabel) {
        return jsonEncoder(staticLabels, testLabel, null);
    }

    public static Consumer<Loki4jAppender> jsonEncoder(boolean staticLabels, String testLabel, Layout<ILoggingEvent> msgLayout) {
        return appender -> {
            appender.setWriter(PipelineConfig.json);
            configEncoder(
                appender.getEncoder(),
                labelCfg("test=" + testLabel + ",level=%level,service_name=my-app", ",", "=", true, false),
                msgLayout,
                staticLabels);
        };
    }

    public static Consumer<Loki4jAppender> protobufEncoder(boolean staticLabels, String testLabel) {
        return protobufEncoder(staticLabels, testLabel, null);
    }

    public static Consumer<Loki4jAppender> protobufEncoder(boolean staticLabels, String testLabel, Layout<ILoggingEvent> msgLayout) {
        return appender -> {
            appender.setWriter(PipelineConfig.protobuf);
            configEncoder(
                appender.getEncoder(),
                labelCfg("test=" + testLabel + ",level=%level,service_name=my-app", ",", "=", true, false),
                msgLayout,
                staticLabels);
        };
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

    public static Consumer<Loki4jAppender> defaultToStringEncoder() {
        return stringEncoder(
            labelCfg("level=%level,app=my-app", ",", "=", true, false),
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
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

    public static Writer stringWriter(int capacity, ByteBufferFactory bufferFactory) {
        return new StringWriter(capacity, bufferFactory);
    }

    public static void configEncoder(
            AbstractLoki4jEncoder encoder,
            AbstractLoki4jEncoder.LabelCfg label,
            Layout<ILoggingEvent> messageLayout,
            boolean staticLabels) {
        encoder.setLabel(label);
        encoder.setMessage(messageLayout);
        encoder.setStaticLabels(staticLabels);
    }

    public static Consumer<Loki4jAppender> stringEncoder(
            AbstractLoki4jEncoder.LabelCfg label,
            Layout<ILoggingEvent> messageLayout,
            boolean staticLabels) {
        return appender -> {
            appender.setWriter(new PipelineConfig.WriterFactory(Generators::stringWriter, "text/plain"));
            configEncoder(
                appender.getEncoder(),
                label,
                messageLayout,
                staticLabels);
        };
    }

    public static AbstractLoki4jEncoder.LabelCfg labelCfg(
            String pattern,
            String pairSeparator,
            String keyValueSeparator,
            boolean nopex,
            boolean readMarkers) {
        var label = new AbstractLoki4jEncoder.LabelCfg();
        label.setPattern(pattern);
        label.setPairSeparator(pairSeparator);
        label.setKeyValueSeparator(keyValueSeparator);
        label.setReadMarkers(readMarkers);
        return label;
    }

    public static AbstractLoki4jEncoder.LabelCfg labelMetadataCfg(
            String pattern,
            String metadataPattern,
            boolean readMarkers) {
        var label = new AbstractLoki4jEncoder.LabelCfg();
        label.setPattern(pattern);
        label.setStructuredMetadataPattern(metadataPattern);
        label.setReadMarkers(readMarkers);
        return label;
    }

    public static PatternLayout plainTextMsgLayout(String pattern) {
        var message = new PatternLayout();
        message.setPattern(pattern);
        return message;
    }

    public static JsonLayout jsonMsgLayout() {
        return new JsonLayout();
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
            Throwable throwable,
            List<Marker> markers) {
        var e = new LoggingEvent();
        e.setTimeStamp(timestamp);
        e.setLevel(level);
        e.setLoggerName(className);
        e.setThreadName(threadName);
        e.setMessage(message);
        e.setMDCPropertyMap(new HashMap<>());
        if (throwable != null)
            e.setThrowableProxy(new ThrowableProxy(throwable));
        if (markers != null)
            markers.forEach(e::addMarker);
        return e;
    }

    public static LoggingEvent loggingEvent(
            long timestamp,
            Level level,
            String className,
            String threadName,
            String message,
            Throwable throwable) {
        return loggingEvent(timestamp, level, className, threadName, message, throwable, null);
    }

    public static LogRecord eventToRecord(ILoggingEvent e, Loki4jEncoder enc) {
        return LogRecord.create(
            e.getTimeStamp(),
            0,
            enc.eventToStream(e),
            enc.eventToMessage(e),
            enc.eventToMetadata(e));
    }

    public static class AppenderWrapper {
        private Loki4jAppender appender;
        public AppenderWrapper(Loki4jAppender appender) {
            this.appender = appender;
        }
        public void append(ILoggingEvent event) {
            appender.append(event);
        }
        public void append(ILoggingEvent... events) {
            for (int i = 0; i < events.length; i++) {
                appender.append(events[i]);
            }
        }
        public void waitAllAppended() {
            appender.waitSendQueueIsEmpty(2L * 60 * 1000);
        }
        public void stop() {
            appender.stop();
        }
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

    public static class DummyHttpSender extends WrappingHttpSender<DummyHttpClient> {

        public DummyHttpSender() {
            super(new DummyHttpClient());
        }

        public SendInvocation captureSendInvocation() {
            return client.captureSendInvocation();
        }

        public byte[] lastSendData() {
            return client.lastSendData();
        }
    }

    public static class WrappingHttpSender<T extends Loki4jHttpClient> extends AbstractHttpSender {
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
