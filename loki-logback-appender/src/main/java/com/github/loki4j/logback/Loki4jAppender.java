package com.github.loki4j.logback;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.github.loki4j.client.pipeline.AsyncBufferPipeline;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.pipeline.PipelineConfig.Builder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki.
 */
public class Loki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int MAX_RETRIES = 30;
    /**
     * A day in milliseconds.
     */
    private static final long MAX_RETRY_TIMEOUT_MS = 86_400_000L;
    /**
     * Max number of events to put into a single batch before sending it to Loki.
     */
    private int batchMaxItems = 1000;
    /**
     * Max number of bytes a single batch can contain (as counted by Loki).
     * This value should not be greater than server.grpc_server_max_recv_msg_size
     * in your Loki config.
     */
    private int batchMaxBytes = 4 * 1024 * 1024;
    /**
     * Max time in milliseconds to keep a batch before sending it to Loki, even if
     * max items/bytes limits for this batch are not reached.
     */
    private long batchTimeoutMs = 60 * 1000;

    /**
     * Max number of bytes to keep in the send queue.
     * When the queue is full, incoming log events are dropped.
     */
    private long sendQueueMaxBytes = batchMaxBytes * 10;

    /**
     * Max number of attempts to send a batch to Loki before it will be dropped.
     * A failed batch send could be retried only in case of ConnectException or 503 status from Loki.
     * All other exceptions and 4xx-5xx statuses do not cause a retry in order to avoid duplicates.
     */
    private int maxRetries = 2;

    /**
     * Time in milliseconds to wait before the next attempt to re-send a failed batch.
     */
    private long retryTimeoutMs = 60 * 1000;

    /**
     * Upper bound in milliseconds for a jitter added to the retries.
     */
    private int maxJitterMs = 1000;

    /**
     * Disables retries of batches that Loki responds to with a 429 status code (TooManyRequests).
     * This reduces impacts on batches from other tenants, which could end up being delayed or dropped
     * due to backoff.
     */
    private boolean dropRateLimitedBatches = false;

    /**
     * A timeout for Loki4j threads to sleep if encode or send queues are empty.
     * Decreasing this value means lower latency at cost of higher CPU usage.
     */
    private long internalQueuesCheckTimeoutMs = 25;

    /**
     * If true, the appender will print its own debug logs to stderr.
     */
    private boolean verbose = false;

    /**
     * If true, the appender will report its metrics using Micrometer.
     */
    private boolean metricsEnabled = false;

    /**
     * If true, the appender will try to send all the remaining events on shutdown,
     * so the proper shutdown procedure might take longer.
     * Otherwise, the appender will drop the unsent events.
     */
    private boolean drainOnStop = true;

    /**
     * Use off-heap memory for storing intermediate data.
     */
    private boolean useDirectBuffers = true;

    /**
     * An encoder to use for converting log record batches to format acceptable by Loki.
     */
    private Loki4jEncoder encoder;

    /**
     * A configurator for HTTP sender.
     */
    private HttpSender sender;

    /**
     * A pipeline that does all the heavy lifting log records processing.
     */
    private AsyncBufferPipeline pipeline;

    /**
     * A counter for events dropped due to backpressure.
     */
    private AtomicLong droppedEventsCount = new AtomicLong(0L);

    private PipelineConfig.Builder pipelineBuilder = PipelineConfig.builder();

    @Override
    public void start() {
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        addInfo(String.format("Starting with " +
            "batchMaxItems=%s, batchMaxBytes=%s, batchTimeout=%s, sendQueueMaxBytes=%s...",
            batchMaxItems, batchMaxBytes, batchTimeoutMs, sendQueueMaxBytes));

        if (sendQueueMaxBytes < batchMaxBytes * 5) {
            addWarn("Configured value sendQueueMaxBytes=" + sendQueueMaxBytes + " is less than `batchMaxBytes * 5`");
            sendQueueMaxBytes = batchMaxBytes * 5;
        }

        if (encoder == null) {
            addWarn("No encoder specified in the config. Using JsonEncoder with default settings");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        if (sender == null) {
            addWarn("No sender specified in the config. Trying to use JavaHttpSender with default settings");
            sender = new JavaHttpSender();
        }

        PipelineConfig pipelineConf = pipelineBuilder
            .setName(this.getName() == null ? "none" : this.getName())
            .setBatchMaxItems(batchMaxItems)
            .setBatchMaxBytes(batchMaxBytes)
            .setBatchTimeoutMs(batchTimeoutMs)
            .setSortByTime(encoder.getSortByTime())
            .setStaticLabels(encoder.getStaticLabels())
            .setSendQueueMaxBytes(sendQueueMaxBytes)
            .setMaxRetries(maxRetries)
            .setRetryTimeoutMs(retryTimeoutMs)
            .setMaxJitterMs(maxJitterMs)
            .setDropRateLimitedBatches(dropRateLimitedBatches)
            .setInternalQueuesCheckTimeoutMs(internalQueuesCheckTimeoutMs)
            .setUseDirectBuffers(useDirectBuffers)
            .setDrainOnStop(drainOnStop)
            .setMetricsEnabled(metricsEnabled)
            .setWriter(encoder.getWriterFactory())
            .setHttpConfig(sender.getConfig())
            .setHttpClientFactory(sender.getHttpClientFactory())
            .setInternalLoggingFactory(source -> new InternalLogger(source, this))
            .build();

        pipeline = new AsyncBufferPipeline(pipelineConf);
        pipeline.start();

        super.start();

        addInfo("Successfully started");
    }

    @Override
    public void stop() {
        if (!super.isStarted()) {
            return;
        }
        addInfo("Stopping...");

        super.stop();

        pipeline.stop();
        encoder.stop();

        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        var appended = pipeline.append(
            event.getTimeStamp(),
            encoder.timestampToNanos(event.getTimeStamp()),
            () -> encoder.eventToStream(event),
            () -> encoder.eventToMessage(event));

        if (!appended)
            reportDroppedEvents();
    }

    private void reportDroppedEvents() {
        var dropped = droppedEventsCount.incrementAndGet();
        if (dropped == 1
                || (dropped <= 90 && dropped % 20 == 0)
                || (dropped <= 900 && dropped % 100 == 0)
                || (dropped <= 900_000 && dropped % 1000 == 0)
                || (dropped <= 9_000_000 && dropped % 10_000 == 0)
                || (dropped <= 900_000_000 && dropped % 1_000_000 == 0)
                || dropped > 1_000_000_000) {
            addWarn(String.format(
                "Backpressure: %s messages dropped. Check `sendQueueSizeBytes` setting", dropped));
            if (dropped > 1_000_000_000) {
                addWarn(String.format(
                    "Resetting dropped message counter from %s to 0", dropped));
                droppedEventsCount.set(0L);
            }
        }
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        pipeline.waitSendQueueIsEmpty(timeoutMs);
    }

    long droppedEventsCount() {
        return droppedEventsCount.get();
    }

    public void setBatchMaxItems(int batchMaxItems) {
        this.batchMaxItems = batchMaxItems;
    }
    public void setBatchMaxBytes(int batchMaxBytes) {
        this.batchMaxBytes = batchMaxBytes;
    }
    public void setBatchTimeoutMs(long batchTimeoutMs) {
        this.batchTimeoutMs = batchTimeoutMs;
    }
    public void setSendQueueMaxBytes(long sendQueueMaxBytes) {
        this.sendQueueMaxBytes = sendQueueMaxBytes;
    }

    public void setMaxRetries(int maxRetries) {
        if (maxRetries > MAX_RETRIES) {
            addWarn("Invalid value for `maxRetries`, using " + MAX_RETRIES + " instead.");
            this.maxRetries = MAX_RETRIES;
        } else {
            this.maxRetries = maxRetries;
        }
    }
    public void setRetryTimeoutMs(long retryTimeoutMs) {
        if (retryTimeoutMs > MAX_RETRY_TIMEOUT_MS) {
            addWarn("Invalid value for `retryTimeoutMs`, using " + MAX_RETRY_TIMEOUT_MS + " instead.");
            this.retryTimeoutMs = MAX_RETRY_TIMEOUT_MS;
        } else {
            this.retryTimeoutMs = retryTimeoutMs;
        }
    }

    public void setMaxJitterMs(int maxJitterMs) {
        this.maxJitterMs = maxJitterMs;
    }

    public void setDropRateLimitedBatches(boolean dropRateLimitedBatches) {
        this.dropRateLimitedBatches = dropRateLimitedBatches;
    }

    /**
     * "format" instead of "encoder" in the name allows to specify
     * the default implementation, so users don't have to write
     * full-qualified class name by default.
     */
    @DefaultClass(JsonEncoder.class)
    public void setFormat(Loki4jEncoder encoder) {
        this.encoder = encoder;
    }

    HttpSender getSender() {
        return sender;
    }

    /**
     * "http" instead of "sender" is just to have a more clear name
     * for the configuration section.
     */
    @DefaultClass(JavaHttpSender.class)
    public void setHttp(HttpSender sender) {
        this.sender = sender;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
    public void setDrainOnStop(boolean drainOnStop) {
        this.drainOnStop = drainOnStop;
    }
    public void setUseDirectBuffers(boolean useDirectBuffers) {
        this.useDirectBuffers = useDirectBuffers;
    }

    /**
     * Sets the PipelineBuilder, this method is intended for testing
     * purpose exclusively.
     *
     * @param pipelineBuilder the pipeline builder
     */
    void setPipelineBuilder(Builder pipelineBuilder) {
        this.pipelineBuilder = Objects.requireNonNull(pipelineBuilder);
    }

}
