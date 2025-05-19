package com.github.loki4j.logback;

import java.util.Optional;

import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;

/**
 * This appender layer is responsible for the configuration required to create a
 * pipeline for sending logs to Loki.
 */
public abstract class PipelineConfigAppenderBase extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /**
     * "batch" section of the appender's config.
     * It contains settings that control grouping log records into batches.
     */
    private BatchCfg batch = new BatchCfg();

    /**
     * "http" section of the appender's config.
     * It contains various settings for sending batches to Loki via HTTP.
     */
    private HttpCfg http = new HttpCfg();

    /**
     * If true, the appender will report its metrics using Micrometer.
     */
    private boolean metricsEnabled = false;

    protected boolean isStaticLabels() {
        return batch.staticLabels;
    }

    protected PipelineConfig buildPipelineConfig() {
        addInfo(String.format("Starting with " +
            "batchMaxItems=%s, batchMaxBytes=%s, batchTimeout=%s, sendQueueMaxBytes=%s...",
            batch.maxItems, batch.maxBytes, batch.timeoutMs, batch.sendQueueMaxBytes));

        var effectiveSendQueueMaxBytes = batch.sendQueueMaxBytes;
        if (effectiveSendQueueMaxBytes < batch.maxBytes * 5) {
            addWarn("Configured value sendQueueMaxBytes=" + effectiveSendQueueMaxBytes + " is less than `batchMaxBytes * 5`");
            effectiveSendQueueMaxBytes = batch.maxBytes * 5;
        }

        var effectiveWriter = http.writer;
        if (effectiveWriter == null) {
            effectiveWriter = http.useProtobufApi ? PipelineConfig.protobuf : PipelineConfig.json;
        }

        var effectiveSender = http.sender;
        if (effectiveSender == null) {
            effectiveSender = new JavaHttpSender();
        }

        return PipelineConfig.builder()
                .setName(this.getName() == null ? "none" : this.getName())
                .setBatchMaxItems(batch.maxItems)
                .setBatchMaxBytes(batch.maxBytes)
                .setBatchTimeoutMs(batch.timeoutMs)
                .setStaticLabels(batch.staticLabels)
                .setSendQueueMaxBytes(effectiveSendQueueMaxBytes)
                .setInternalQueuesCheckTimeoutMs(batch.internalQueuesCheckTimeoutMs)
                .setUseDirectBuffers(batch.useDirectBuffers)
                .setDrainOnStop(batch.drainOnStop)
                .setMaxRetries(http.maxRetries)
                .setMinRetryBackoffMs(http.minRetryBackoffMs)
                .setMaxRetryBackoffMs(http.maxRetryBackoffMs)
                .setMaxRetryJitterMs(http.maxRetryJitterMs)
                .setDropRateLimitedBatches(http.dropRateLimitedBatches)
                .setMetricsEnabled(metricsEnabled)
                .setWriter(effectiveWriter)
                .setHttpConfig(effectiveSender.getConfig().fill(this::fillHttpConfig))
                .setHttpClientFactory(effectiveSender.getHttpClientFactory())
                .setInternalLoggingFactory(source -> new InternalLogger(source, this))
                .build();
    }

    private void fillHttpConfig(HttpConfig.Builder builder) {
        builder
            .setPushUrl(http.url)
            .setTenantId(http.tenantId)
            .setConnectionTimeoutMs(http.connectionTimeoutMs)
            .setRequestTimeoutMs(http.requestTimeoutMs)
            .setUsername(Optional.ofNullable(http.auth).map(a -> a.username))
            .setPassword(Optional.ofNullable(http.auth).map(a -> a.password));
    }

    /** Keeping getter for testing purposes */
    BatchCfg getBatch() {
        return batch;
    }
    public void setBatch(BatchCfg batch) {
        this.batch = batch;
    }

    /** Keeping getter for testing purposes */
    HttpCfg getHttp() {
        return http;
    }
    public void setHttp(HttpCfg http) {
        this.http = http;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }


    public static final class BatchCfg {
        /**
         * Max number of events to put into a single batch before sending it to Loki.
         */
        int maxItems = 1000;
        /**
         * Max number of bytes a single batch can contain (as counted by Loki).
         * This value should not be greater than server.grpc_server_max_recv_msg_size
         * in your Loki config.
         */
        int maxBytes = 4 * 1024 * 1024;
        /**
         * Max time in milliseconds to keep a batch before sending it to Loki, even if
         * max items/bytes limits for this batch are not reached.
         */
        long timeoutMs = 60 * 1000;

        /**
         * Max number of bytes to keep in the send queue.
         * When the queue is full, incoming log events are dropped.
         */
        long sendQueueMaxBytes = maxBytes * 10;

        /**
         * A timeout for Loki4j threads to sleep if encode or send queues are empty.
         * Decreasing this value means lower latency at cost of higher CPU usage.
         */
        long internalQueuesCheckTimeoutMs = 25;

        /**
         * If true, the appender will try to send all the remaining events on shutdown,
         * so the proper shutdown procedure might take longer.
         * Otherwise, the appender will drop the unsent events.
         */
        boolean drainOnStop = true;

        /**
         * Use off-heap memory for storing intermediate data.
         */
        boolean useDirectBuffers = true;

        /**
         * If true, labels will be calculated only once for the first log record
         * and then used for all other log records without re-calculation.
         * Otherwise, they will be calculated for each record individually.
         */
        boolean staticLabels = false;


        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }
        public void setMaxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
        }
        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
        public void setSendQueueMaxBytes(long sendQueueMaxBytes) {
            this.sendQueueMaxBytes = sendQueueMaxBytes;
        }
        public void setInternalQueuesCheckTimeoutMs(long internalQueuesCheckTimeoutMs) {
            this.internalQueuesCheckTimeoutMs = internalQueuesCheckTimeoutMs;
        }
        public void setDrainOnStop(boolean drainOnStop) {
            this.drainOnStop = drainOnStop;
        }
        public void setUseDirectBuffers(boolean useDirectBuffers) {
            this.useDirectBuffers = useDirectBuffers;
        }
        public void setStaticLabels(boolean staticLabels) {
            this.staticLabels = staticLabels;
        }
    }

    public static final class HttpCfg {

        /**
        * Loki endpoint to be used for sending batches
        */
        private String url = "http://localhost:3100/loki/api/v1/push";

        /**
         * Tenant identifier.
         * It is required only for sending logs directly to Loki operating in multi-tenant mode.
         * Otherwise this setting has no effect
         */
        private Optional<String> tenantId = Optional.empty();

        /**
         * Time in milliseconds to wait for HTTP connection to Loki to be established
         * before reporting an error
         */
        private long connectionTimeoutMs = 30_000;

        /**
         * Time in milliseconds to wait for HTTP request to Loki to be responded
         * before reporting an error
         */
        private long requestTimeoutMs = 5_000;

        /**
         * Optional creds for basic HTTP auth
         */
        private BasicAuth auth;

        /**
         * Max number of attempts to send a batch to Loki before it will be dropped.
         * A failed batch send could be retried only in case of ConnectException, or
         * receiving statuses 429, 503 from Loki.
         * All other exceptions and 4xx-5xx statuses do not cause a retry in order to
         * avoid duplicates.
         */
        int maxRetries = 2;

        /**
         * Initial backoff delay before the next attempt to re-send a failed batch.
         * Batches are retried with an exponential backoff (e.g. 0.5s, 1s, 2s, 4s, etc.)
         * and jitter.
         */
        long minRetryBackoffMs = 500;

        /**
         * Maximum backoff delay before the next attempt to re-send a failed batch.
         */
        long maxRetryBackoffMs = 60 * 1000;

        /**
         * Upper bound for a jitter added to the retry delays.
         */
        int maxRetryJitterMs = 500;

        /**
         * If true, batches that Loki responds to with a 429 status code
         * (TooManyRequests)
         * will be dropped rather than retried.
         */
        boolean dropRateLimitedBatches = false;

        /**
         * If true, Loki4j uses Protobuf Loki API instead of JSON.
         */
        boolean useProtobufApi = false;

        /**
         * A writer to use for converting log record batches to format acceptable by
         * Loki.
         */
        private WriterFactory writer;

        /**
         * A configurator for HTTP sender.
         */
        private HttpSender sender;

        public void setUrl(String url) {
            this.url = url;
        }
        public void setAuth(BasicAuth auth) {
            this.auth = auth;
        }
        public void setTenantId(String tenant) {
            this.tenantId = Optional.ofNullable(tenant);
        }
        public void setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
        public void setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        public void setMinRetryBackoffMs(long minRetryBackoffMs) {
            this.minRetryBackoffMs = minRetryBackoffMs;
        }
        public void setMaxRetryBackoffMs(long maxRetryBackoffMs) {
            this.maxRetryBackoffMs = maxRetryBackoffMs;
        }
        public void setMaxRetryJitterMs(int maxRetryJitterMs) {
            this.maxRetryJitterMs = maxRetryJitterMs;
        }
        public void setDropRateLimitedBatches(boolean dropRateLimitedBatches) {
            this.dropRateLimitedBatches = dropRateLimitedBatches;
        }
        public void setUseProtobufApi(boolean useProtobufApi) {
            this.useProtobufApi = useProtobufApi;
        }

        @DefaultClass(JavaHttpSender.class)
        public void setSender(HttpSender sender) {
            this.sender = sender;
        }

        /** Keeping this for test purpose */
        void setWriter(WriterFactory writer) {
            this.writer = writer;
        }
    }

    public static final class BasicAuth {
        /**
         * Username to use for basic auth
         */
        String username;
        /**
         * Password to use for basic auth
         */
        String password;

        public void setUsername(String username) {
            this.username = username;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
