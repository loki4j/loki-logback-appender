package com.github.loki4j.client.pipeline;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.loki4j.client.http.ApacheHttpClient;
import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.JavaHttpClient;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.util.Loki4jLogger;
import com.github.loki4j.client.writer.JsonWriter;
import com.github.loki4j.client.writer.ProtobufWriter;
import com.github.loki4j.client.writer.Writer;

/**
 * Configuration properties for Loki4j pipeline.
 */
public class PipelineConfig {

    public static final WriterFactory json = new WriterFactory(
        (capacity, bufferFactory) -> new JsonWriter(capacity),
        "application/json");

    public static final WriterFactory protobuf = new WriterFactory(
        (capacity, bbFactory) -> new ProtobufWriter(capacity, bbFactory),
        "application/x-protobuf");

    public static final Function<HttpConfig, Loki4jHttpClient> defaultHttpClientFactory = cfg ->
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
     * Name of this pipeline.
     */
    public final String name;

    /**
     * Max number of events to put into a single batch before sending it to Loki.
     */
    public final int batchMaxItems;

    /**
     * Max number of bytes a single batch can contain (as counted by Loki).
     * This value should not be greater than server.grpc_server_max_recv_msg_size
     * in your Loki config.
     */
    public final int batchMaxBytes;

    /**
     * Max time in milliseconds to keep a batch before sending it to Loki, even if
     * max items/bytes limits for this batch are not reached.
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
     * When the queue is full, incoming log events are dropped.
     */
    public final long sendQueueMaxBytes;

    /**
     * Max number of attempts to send a batch to Loki before it will be dropped.
     * A failed batch send could be retried only in case of ConnectException, or receiving statuses 429, 503 from Loki.
     * All other exceptions and 4xx-5xx statuses do not cause a retry in order to avoid duplicates.
     */
    public final int maxRetries;

    /**
     * Initial backoff delay before the next attempt to re-send a failed batch.
     * Batches are retried with an exponential backoff (e.g. 0.5s, 1s, 2s, 4s, etc.) and jitter.
     */
    public final long minRetryBackoffMs;

    /**
     * Maximum backoff delay before the next attempt to re-send a failed batch.
     */
    public final long maxRetryBackoffMs;

    /**
     * Upper bound for a jitter added to the retry delays.
     */
    public final int maxRetryJitterMs;

    /**
     * If true, batches that Loki responds to with a 429 status code (TooManyRequests)
     * will be dropped rather than retried.
     */
    public final boolean dropRateLimitedBatches;

    /**
     * A timeout for Loki4j threads to sleep if encode or send queues are empty.
     * Decreasing this value means lower latency at cost of higher CPU usage.
     */
    public final long internalQueuesCheckTimeoutMs;

    /**
     * Use off-heap memory for storing intermediate data.
     */
    public final boolean useDirectBuffers;

    /**
     * If true, the pipeline will try to send all the remaining events on shutdown,
     * so the proper shutdown procedure might take longer.
     * Otherwise, the pipeline will drop the unsent events.
     */
    public final boolean drainOnStop;

    /**
     * If true, the pipeline will report its metrics using Micrometer.
     */
    public final boolean metricsEnabled;

    /**
     * A factory for Writer.
    */
    public final WriterFactory writerFactory;

    /**
     * Configuration properties for HTTP clients.
     */
    public final HttpConfig httpConfig;

    /**
     * A factory for HTTP client for sending logs to Loki.
     * Argument is a config required for constructing an HTTP client.
     */
    public final Function<HttpConfig, Loki4jHttpClient> httpClientFactory;

    /**
     * A factory for an internal logger.
     * Argument is a source class to report log messages from.
     */
    public final Function<Object, Loki4jLogger> internalLoggingFactory;

    private PipelineConfig(
            String name,
            int batchMaxItems,
            int batchMaxBytes,
            long batchTimeoutMs,
            boolean sortByTime,
            boolean staticLabels,
            long sendQueueMaxBytes,
            int maxRetries,
            long minRetryBackoffMs,
            long maxRetryBackoffMs,
            int maxRetryJitterMs,
            boolean dropRateLimitedBatches,
            long internalQueuesCheckTimeoutMs,
            boolean useDirectBuffers,
            boolean drainOnStop,
            boolean metricsEnabled,
            WriterFactory writerFactory,
            HttpConfig httpConfig, Function<HttpConfig, Loki4jHttpClient> httpClientFactory,
            Function<Object, Loki4jLogger> internalLoggingFactory) {
        this.name = name;
        this.batchMaxItems = batchMaxItems;
        this.batchMaxBytes = batchMaxBytes;
        this.batchTimeoutMs = batchTimeoutMs;
        this.sortByTime = sortByTime;
        this.staticLabels = staticLabels;
        this.sendQueueMaxBytes = sendQueueMaxBytes;
        this.maxRetries = maxRetries;
        this.minRetryBackoffMs = minRetryBackoffMs;
        this.maxRetryBackoffMs = maxRetryBackoffMs;
        this.maxRetryJitterMs = maxRetryJitterMs;
        this.dropRateLimitedBatches = dropRateLimitedBatches;
        this.internalQueuesCheckTimeoutMs = internalQueuesCheckTimeoutMs;
        this.useDirectBuffers = useDirectBuffers;
        this.drainOnStop = drainOnStop;
        this.metricsEnabled = metricsEnabled;
        this.writerFactory = writerFactory;
        this.httpConfig = httpConfig;
        this.httpClientFactory = httpClientFactory;
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
        private int maxRetries = 2;
        private long minRetryBackoffMs = 500;
        private long maxRetryBackoffMs = 60 * 1000;
        private int maxRetryJitterMs = 500;
        private boolean dropRateLimitedBatches = false;
        private long internalQueuesCheckTimeoutMs = 25;
        private boolean useDirectBuffers = true;
        private boolean drainOnStop = true;
        private boolean metricsEnabled = false;
        private WriterFactory writer = json;
        private HttpConfig.Builder httpConfigBuilder = java(5 * 60_000);
        private Function<HttpConfig, Loki4jHttpClient> httpClientFactory = defaultHttpClientFactory;
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
                    maxRetries,
                    minRetryBackoffMs,
                    maxRetryBackoffMs,
                    maxRetryJitterMs,
                    dropRateLimitedBatches,
                    internalQueuesCheckTimeoutMs,
                    useDirectBuffers,
                    drainOnStop,
                    metricsEnabled,
                    writer,
                    httpConfigBuilder.build(writer.contentType),
                    httpClientFactory,
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

        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder setMinRetryBackoffMs(long minRetryBackoffMs) {
            this.minRetryBackoffMs = minRetryBackoffMs;
            return this;
        }

        public Builder setMaxRetryBackoffMs(long maxRetryBackoffMs) {
            this.maxRetryBackoffMs = maxRetryBackoffMs;
            return this;
        }

        public Builder setMaxRetryJitterMs(int maxRetryJitterMs) {
            this.maxRetryJitterMs = maxRetryJitterMs;
            return this;
        }

        public Builder setDropRateLimitedBatches(boolean dropRateLimitedBatches) {
            this.dropRateLimitedBatches = dropRateLimitedBatches;
            return this;
        }

        public Builder setInternalQueuesCheckTimeoutMs(long internalQueuesCheckTimeoutMs) {
            this.internalQueuesCheckTimeoutMs = internalQueuesCheckTimeoutMs;
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

        public Builder setMetricsEnabled(boolean metricsEnabled) {
            this.metricsEnabled = metricsEnabled;
            return this;
        }

        public Builder setWriter(WriterFactory writer) {
            this.writer = writer;
            return this;
        }

        public Builder setHttpConfig(HttpConfig.Builder httpConfigBuilder) {
            this.httpConfigBuilder = httpConfigBuilder;
            return this;
        }

        public Builder setHttpClientFactory(Function<HttpConfig, Loki4jHttpClient> httpClientFactory) {
            this.httpClientFactory = httpClientFactory;
            return this;
        }

        public Builder setInternalLoggingFactory(Function<Object, Loki4jLogger> internalLoggingFactory) {
            this.internalLoggingFactory = internalLoggingFactory;
            return this;
        }

    }

    /**
     * A factory for Writer.
     */
    public static class WriterFactory {

        /**
         * A factory for creating a Writer instance.
         * First argument is a capacity of the writer buffer.
         * Second argument is a factory for byte buffers.
         */
        public final BiFunction<Integer, ByteBufferFactory, Writer> factory;

        /**
         * HTTP content-type generated by this Writer.
         */
        public final String contentType;

        public WriterFactory(BiFunction<Integer, ByteBufferFactory, Writer> factory, String contentType) {
            this.factory = factory;
            this.contentType = contentType;
        }
    }

}
