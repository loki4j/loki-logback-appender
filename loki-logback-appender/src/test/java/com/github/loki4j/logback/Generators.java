package com.github.loki4j.logback;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.slf4j.Marker;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.http.LokiResponse;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.writer.Writer;
import com.github.loki4j.testkit.dummy.ExceptionGenerator;
import com.github.loki4j.testkit.dummy.LokiHttpServerMock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static com.github.loki4j.testkit.dummy.Generators.genMessage;

public class Generators {

    private static String LABELS_MESSAGE_SEPARATOR = " %%% ";

    static HttpConfig.Builder defaultHttpConfig = HttpConfig.builder();

    public static String batchToString(LogRecordBatch batch) {
        var s = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            var b = batch.get(i);
            s
                .append(Arrays.toString(b.stream.labels))
                .append(LABELS_MESSAGE_SEPARATOR)
                .append("ts=")
                .append(b.timestampMs)
                .append(" ")
                .append(b.message)
                .append('\n');
        }
        return s.toString();
    }

    public static String batchToString(LogRecord[] records) {
        return batchToString(new LogRecordBatch(records));
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
        appender.setBatchMaxItems(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        appender.setFormat(encoder);
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

    public static JsonEncoder jsonEncoder(boolean staticLabels, String testLabel) {
        var encoder = new JsonEncoder();
        encoder.setStaticLabels(staticLabels);
        encoder.setLabel(labelCfg("test=" + testLabel + ",level=%level,app=my-app", ",", "=", true, false));
        encoder.setSortByTime(true);
        return encoder;
    }

    public static ProtobufEncoder protobufEncoder(boolean staticLabels, String testLabel) {
        var encoder = new ProtobufEncoder();
        encoder.setStaticLabels(staticLabels);
        encoder.setLabel(labelCfg("test=" + testLabel + ",level=%level,app=my-app", ",", "=", true, false));
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
            labelCfg("level=%level,app=my-app", ",", "=", true, false),
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

    public static Writer stringWriter(int capacity, ByteBufferFactory bufferFactory) {
        return new StringWriter(capacity, bufferFactory);
    }

    public static AbstractLoki4jEncoder wrapToEncoder(
            BiFunction<Integer, ByteBufferFactory, Writer> writerFactory,
            AbstractLoki4jEncoder.LabelCfg label,
            AbstractLoki4jEncoder.MessageCfg message,
            boolean sortByTime,
            boolean staticLabels) {
        var encoder = new AbstractLoki4jEncoder() {
            @Override
            public PipelineConfig.WriterFactory getWriterFactory() {
                return new PipelineConfig.WriterFactory(writerFactory, "text/plain");
            }
        };
        encoder.setLabel(label);
        encoder.setMessage(message);
        encoder.setSortByTime(sortByTime);
        encoder.setStaticLabels(staticLabels);
        return encoder;
    }

    public static AbstractLoki4jEncoder toStringEncoder(
            AbstractLoki4jEncoder.LabelCfg label,
            AbstractLoki4jEncoder.MessageCfg message,
            boolean sortByTime,
            boolean staticLabels) {
        return wrapToEncoder(Generators::stringWriter, label, message, sortByTime, staticLabels);
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
        label.setNopex(nopex);
        label.setReadMarkers(readMarkers);
        return label;
    }

    public static AbstractLoki4jEncoder.MessageCfg messageCfg(
            String pattern) {
        var message = new AbstractLoki4jEncoder.MessageCfg();
        message.setPattern(pattern);
        return message;
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
            Marker marker) {
        var e = new LoggingEvent();
        e.setTimeStamp(timestamp);
        e.setLevel(level);
        e.setLoggerName(className);
        e.setThreadName(threadName);
        e.setMessage(message);
        if (throwable != null)
            e.setThrowableProxy(new ThrowableProxy(throwable));
        if (marker != null)
            e.setMarker(marker);
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
            enc.eventToMessage(e));
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

    public static class StringWriter implements Writer {
        private final ByteBufferFactory bf;
        private ByteBuffer b;
        private int size = 0;
        public StringWriter(int capacity, ByteBufferFactory bufferFactory) {
            bf = bufferFactory;
            b = bf.allocate(capacity);
        }
        @Override
        public void serializeBatch(LogRecordBatch batch) {
            b.clear();
            var str = batchToString(batch);
            var data = str.getBytes(StandardCharsets.UTF_8);
            if (b.capacity() < data.length) b = bf.allocate(data.length);
            b.put(data);
            b.flip();
            size = data.length;
        }
        @Override
        public int size() {
            return size;
        }
        @Override
        public void toByteBuffer(ByteBuffer buffer) {
            buffer.put(b);
            buffer.flip();
        }
        @Override
        public byte[] toByteArray() {
            byte[] r = new byte[b.remaining()];
            b.get(r);
            return r;
        }
        @Override
        public void reset() {
            size = 0;
            b.clear();
        }
        @Override
        public boolean isBinary() {
            return false;
        }
    }

    public static class FailingStringWriter extends StringWriter {
        public AtomicBoolean fail = new AtomicBoolean(false);
        public FailingStringWriter(int capacity, ByteBufferFactory bufferFactory) {
            super(capacity, bufferFactory);
        }
        @Override
        public void serializeBatch(LogRecordBatch batch) {
            if (fail.get())
                throw new RuntimeException("Text exception");
            super.serializeBatch(batch);
        }
    }

    public static class DummyHttpSender extends AbstractHttpSender {
        private final DummyHttpClient client = new DummyHttpClient();

        public byte[] lastBatch() {
            return client.lastBatch;
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

    public static class DummyHttpClient implements Loki4jHttpClient {
        public byte[] lastBatch;

        @Override
        public void close() throws Exception { }

        @Override
        public HttpConfig getConfig() {
            return defaultHttpConfig.build("test");
        }

        @Override
        public LokiResponse send(ByteBuffer batch) throws Exception {
            lastBatch = new byte[batch.remaining()];
            batch.get(lastBatch);
            return new LokiResponse(204, "");
        }
    }

    public static class StoppableHttpClient extends DummyHttpClient {
        public AtomicBoolean wait = new AtomicBoolean(false);
        @Override
        public LokiResponse send(ByteBuffer batch) throws Exception {
            while(wait.get())
                LockSupport.parkNanos(1000);
            return super.send(batch);
        }
    }

    public static class FailingHttpClient extends DummyHttpClient {
        private static final LokiResponse RATE_LIMITED = new LokiResponse(429, "Rate Limited Request");
        public final AtomicBoolean fail = new AtomicBoolean(false);
        public final AtomicBoolean rateLimited = new AtomicBoolean(false);
        public volatile int sendCount = 0;
        @Override
        public LokiResponse send(ByteBuffer batch) throws Exception {
            sendCount++;
            var response = super.send(batch);
            if (fail.get() && !rateLimited.get())
                throw new ConnectException("Text exception");
            else if (fail.get() && rateLimited.get())
                return RATE_LIMITED;
            return response;
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

    public static class StringPayload {
        public final HashMap<String, ArrayList<String>> data;

        private StringPayload(HashMap<String, ArrayList<String>> data) {
            this.data = data;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((data == null) ? 0 : data.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            StringPayload other = (StringPayload) obj;
            if (data == null) {
                if (other.data != null)
                    return false;
            } else if (!data.equals(other.data))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "StringPayload [" + data + "]";
        }

        public static StringPayload parse(String input) {
            HashMap<String, ArrayList<String>> data = new HashMap<>();
            var lines = input.split("\n");
            for (String line : lines) {
                var pair = line.split(LABELS_MESSAGE_SEPARATOR);
                var recs = data.computeIfAbsent(pair[0], x -> new ArrayList<>());
                recs.add(pair[1]);
            }
            return new StringPayload(data);
        }

        public static StringPayload parse(byte[] input) {
            return parse(new String(input));
        }

        public static StringPayload parse(byte[] input, Charset charset) {
            return parse(new String(input, charset));
        }

        public static StringPayloadBuilder builder() {
            return new StringPayloadBuilder();
        }

        public static class StringPayloadBuilder {
            private HashMap<String, ArrayList<String>> data = new HashMap<>();
            public StringPayloadBuilder stream(String stream, String... records) {
                var recs = data.computeIfAbsent(stream, x -> new ArrayList<>());
                recs.addAll(Arrays.asList(records));
                return this;
            }
            public StringPayload build() {
                return new StringPayload(data);
            }
        }
    }

}
