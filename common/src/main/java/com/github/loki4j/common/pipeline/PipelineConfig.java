package com.github.loki4j.common.pipeline;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.loki4j.common.http.ApacheHttpClient;
import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.http.JavaHttpClient;
import com.github.loki4j.common.http.Loki4jHttpClient;
import com.github.loki4j.common.util.ByteBufferFactory;
import com.github.loki4j.common.util.Loki4jLogger;
import com.github.loki4j.common.writer.JsonWriter;
import com.github.loki4j.common.writer.ProtobufWriter;
import com.github.loki4j.common.writer.Writer;

/**
 * Configuration properties for Loki4j pipeline
 */
public class PipelineConfig {

    public static final WriterFactory json = new WriterFactory(
        (capacity, bufferFactory) -> new JsonWriter(capacity),
        "application/json");

    public static final WriterFactory protobuf = new WriterFactory(
        (capacity, bbFactory) -> new ProtobufWriter(capacity, bbFactory),
        "application/x-protobuf");

    public static final Function<HttpConfig, Loki4jHttpClient> defaultSenderFactory = cfg ->
        (cfg.clientSpecific instanceof HttpConfig.JavaHttpConfig)
        ? new JavaHttpClient(cfg)
        : new ApacheHttpClient(cfg);

    public static HttpConfig.Builder apache(int maxConnections, long connectionKeepAliveMs) {
        return HttpConfig.builder()
            .setClientConfig(new HttpConfig.ApacheHttpConfig(maxConnections, connectionKeepAliveMs));
    }

    public static HttpConfig.Builder java(long innerThreadsExpirationMs) {
        return HttpConfig.builder()
            .setClientConfig(new HttpConfig.JavaHttpConfig(innerThreadsExpirationMs));
    }

    /**
     * Name of this pipeline
     */
    public final String name;

    /**
     * Max number of events to put into a single batch before sending it to Loki
     */
    public final int batchMaxItems;

    /**
     * Max number of bytes a single batch (as counted by Loki) can contain.
     * This value should not be greater than server.grpc_server_max_recv_msg_size
     * in your Loki config
     */
    public final int batchMaxBytes;

    /**
     * Max time in milliseconds to wait before sending a batch to Loki, even if that
     * batch isn't full
     */
    public final long batchTimeoutMs;

    /**
     * If true, log records in batch are sorted by timestamp.
     * If false, records will be sent to Loki in arrival order.
     * Turn this on if you see 'entry out of order' error from Loki.
     */
    public final boolean sortByTime;

    /**
     * If you use only one label for all log records, you can
     * set this flag to true and save some CPU time on grouping records by label.
     */
    public final boolean staticLabels;

    /**
     * Max number of bytes to keep in the send queue.
     * When the queue is full, incoming log events are dropped
     */
    public final long sendQueueMaxBytes;

    /**
     * Use off-heap memory for storing intermediate data
     */
    public final boolean useDirectBuffers;

    /**
     * Wait util all remaining events are sent before shutdown the appender
     */
    public final boolean drainOnStop;

    /**
     * A factory for Writer
    */
    public final WriterFactory writerFactory;

    /**
     * Configuration properties for HTTP senders
     */
    public final HttpConfig httpConfig;

    /**
     * A factory for Sender.
     * Argument is a config required for constructing an HTTP client
     */
    public final Function<HttpConfig, Loki4jHttpClient> senderFactory;

    /**
     * A factory for an internal logger.
     * Argument is a source class to report log messages from
     */
    public final Function<Object, Loki4jLogger> internalLoggingFactory;

    public PipelineConfig(String name, int batchMaxItems, int batchMaxBytes, long batchTimeoutMs, boolean sortByTime,
            boolean staticLabels, long sendQueueMaxBytes, boolean useDirectBuffers, boolean drainOnStop,
            WriterFactory writerFactory, HttpConfig httpConfig, Function<HttpConfig, Loki4jHttpClient> senderFactory,
            Function<Object, Loki4jLogger> internalLoggingFactory) {
        this.name = name;
        this.batchMaxItems = batchMaxItems;
        this.batchMaxBytes = batchMaxBytes;
        this.batchTimeoutMs = batchTimeoutMs;
        this.sortByTime = sortByTime;
        this.staticLabels = staticLabels;
        this.sendQueueMaxBytes = sendQueueMaxBytes;
        this.useDirectBuffers = useDirectBuffers;
        this.drainOnStop = drainOnStop;
        this.writerFactory = writerFactory;
        this.httpConfig = httpConfig;
        this.senderFactory = senderFactory;
        this.internalLoggingFactory = internalLoggingFactory;
    }

    public static final Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name = "loki4j";
        private int batchMaxItems = 1000;
        private int batchMaxBytes = 4 * 1024 * 1024;
        private long batchTimeoutMs = 60 * 1000;
        private boolean sortByTime = false;
        private boolean staticLabels = false;
        private long sendQueueMaxBytes = batchMaxBytes * 10;
        private boolean useDirectBuffers = true;
        private boolean drainOnStop = true;
        private WriterFactory writer = json;
        private HttpConfig.Builder httpClient = java(5 * 60_000);
        private Function<HttpConfig, Loki4jHttpClient> senderFactory = defaultSenderFactory;
        private Function<Object, Loki4jLogger> internalLoggingFactory;

        public PipelineConfig build() {
            return new PipelineConfig(
                name,
                batchMaxItems,
                batchMaxBytes,
                batchTimeoutMs,
                sortByTime,
                staticLabels,
                sendQueueMaxBytes,
                useDirectBuffers,
                drainOnStop,
                writer,
                httpClient.build(writer.contentType),
                senderFactory,
                internalLoggingFactory);
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setBatchMaxItems(int batchMaxItems) {
            this.batchMaxItems = batchMaxItems;
            return this;
        }

        public Builder setBatchMaxBytes(int batchMaxBytes) {
            this.batchMaxBytes = batchMaxBytes;
            return this;
        }

        public Builder setBatchTimeoutMs(long batchTimeoutMs) {
            this.batchTimeoutMs = batchTimeoutMs;
            return this;
        }

        public Builder setSortByTime(boolean sortByTime) {
            this.sortByTime = sortByTime;
            return this;
        }

        public Builder setStaticLabels(boolean staticLabels) {
            this.staticLabels = staticLabels;
            return this;
        }

        public Builder setSendQueueMaxBytes(long sendQueueMaxBytes) {
            this.sendQueueMaxBytes = sendQueueMaxBytes;
            return this;
        }

        public Builder setUseDirectBuffers(boolean useDirectBuffers) {
            this.useDirectBuffers = useDirectBuffers;
            return this;
        }

        public Builder setDrainOnStop(boolean drainOnStop) {
            this.drainOnStop = drainOnStop;
            return this;
        }

        public Builder setWriter(WriterFactory writer) {
            this.writer = writer;
            return this;
        }

        public Builder setHttpClient(HttpConfig.Builder httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder setSenderFactory(Function<HttpConfig, Loki4jHttpClient> senderFactory) {
            this.senderFactory = senderFactory;
            return this;
        }

        public Builder setInternalLoggingFactory(Function<Object, Loki4jLogger> internalLoggingFactory) {
            this.internalLoggingFactory = internalLoggingFactory;
            return this;
        }

    }

    /**
     * A factory for Writer
     */
    public static class WriterFactory {

        /**
         * A factory for creating a Writer instance.
         * First argument is a capacity of the writer buffer.
         * Second argument is a factory for byte buffers.
         */
        public final BiFunction<Integer, ByteBufferFactory, Writer> factory;

        /**
         * HTTP content-type generated by this Writer
         */
        public final String contentType;

        public WriterFactory(BiFunction<Integer, ByteBufferFactory, Writer> factory, String contentType) {
            this.factory = factory;
            this.contentType = contentType;
        }
    }

}
