package com.github.loki4j.logback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.loki4j.common.Batcher;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;
import com.github.loki4j.common.SoftLimitBuffer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki
 */
public class OldLoki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final LogRecord[] ZERO_EVENTS = new LogRecord[0];

    /**
     * Max number of events to put into single batch and send to Loki
     */
    private int batchSize = 1000;
    /**
     * Max time in milliseconds to wait before sending a batch to Loki
     */
    private long batchTimeoutMs = 60 * 1000;

    /**
     * Max number of events to keep in the send queue.
     * When the queue id full, incoming log events are dropped
     */
    private int sendQueueSize = 50 * 1000;

    /**
     * Number of threads to use for log message processing and formatting
     */
    private int processingThreads = 1;

    /**
     * If true, the appender will print its own debug logs to stderr
     */
    private boolean verbose = false;

    /**
     * If true, the appender will report its metrics using Micrometer
     */
    private boolean metricsEnabled = false;

    /**
     * An encoder to use for converting log record batches to format acceptable by Loki
     */
    private Loki4jEncoder encoder;

    /**
     * A HTTPS sender to use for pushing logs to Loki
     */
    private HttpSender sender;

    /**
     * A tracker for the appender's metrics (if enabled)
     */
    private LoggerMetrics metrics;

    private SoftLimitBuffer<LogRecord> buffer;
    private Batcher batcher;

    private ScheduledExecutorService scheduler;

    private AtomicLong lastSendTime = new AtomicLong(System.currentTimeMillis());

    @Override
    public void start() {
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        addInfo(String.format("Starting with " +
            "procThreads=%s, batchSize=%s, batchTimeout=%s...",
            processingThreads, batchSize, batchTimeoutMs));

        if (encoder == null) {
            addWarn("No encoder specified in the config. Using JsonEncoder with default settings");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        if (metricsEnabled) {
            var host = context.getProperty(CoreConstants.HOSTNAME_KEY);
            metrics = new LoggerMetrics(
                this.getName() == null ? "none" : this.getName(),
                host == null ? "unknown" : host);
        }

        buffer = new SoftLimitBuffer<>(sendQueueSize);
        batcher = new Batcher(batchSize, batchTimeoutMs);

        scheduler = Executors.newScheduledThreadPool(processingThreads, new LokiThreadFactory("loki-scheduler"));

        if (sender == null) {
            addWarn("No sender specified in the config. Trying to use JavaHttpSender with default settings");
            sender = new JavaHttpSender();
        }
        sender.setContext(context);
        sender.setContentType(encoder.getContentType());
        sender.start();

        super.start();

        scheduler.scheduleAtFixedRate(
            () -> drainAsync(lastSendTime.get()),
            100,
            100,
            TimeUnit.MILLISECONDS);
        addInfo("Successfully started");
    }

    @Override
    public void stop() {
        if (!super.isStarted()) {
            return;
        }
        addInfo("Stopping...");

        super.stop();

        try {
            // drain no matter how much time passed since last batch
            drainAsync(0L).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            addWarn("Error during buffer drain on stop", e);
        }

        encoder.stop();

        scheduler.shutdown();
        

        sender.stop();
        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        appendAsync(event);
    }

    private CompletableFuture<Void> appendAsync(ILoggingEvent event) {
        var startedNs = System.nanoTime();
        var appended = buffer.offer(() -> encoder.eventToRecord(event));
        CompletableFuture<Void> appendResult = appended
            ?   CompletableFuture.supplyAsync(() -> {
                    LogRecord[] batch = ZERO_EVENTS;
                    LogRecord record = buffer.poll();
                    while(record != null && batch.length == 0) {
                        batch = batcher.add(record, ZERO_EVENTS);
                        if (batch.length == 0)
                            record = buffer.poll();
                    }
                    return batch;
                }, scheduler)
                .thenComposeAsync(this::handleBatchAsync, scheduler)
            :   CompletableFuture.completedFuture(null);

        if (metricsEnabled) metrics.eventAppended(startedNs, !appended);
        return appendResult;
    }

    protected byte[] encode(LogRecord[] batch) {
        var startedNs = System.nanoTime();
        var encoded = encoder.encode(batch);
        if (metricsEnabled) metrics.batchEncoded(startedNs, batch.length);
        return encoded;
    }

    protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        var startedNs = System.nanoTime();
        return sender
            .sendAsync(batch)
            .whenComplete((r, e) -> {
                if (metricsEnabled)
                    metrics.batchSent(startedNs, batch.length, e != null || r.status > 299);
            });
    }

    private CompletableFuture<Void> drainAsync(long lastSendTimeMs) {
        return CompletableFuture
            .supplyAsync(() ->
                batcher.drain(lastSendTimeMs, ZERO_EVENTS), scheduler)
            .thenComposeAsync(this::handleBatchAsync, scheduler);
    }

    private CompletableFuture<Void> handleBatchAsync(LogRecord[] batch) {
        if (batch.length == 0)
            return CompletableFuture.completedFuture(null);

        var batchId = System.nanoTime();
        var body = encode(batch);
        addInfo(String.format(
            ">>> Batch #%x: Sending %,d items converted to %,d bytes",
            batchId, batch.length, body.length));
        //try { System.out.write(body); } catch (Exception e) { e.printStackTrace(); }
        //System.out.println("\n");

        return sendAsync(body)
            .whenComplete((r, e) -> {
                buffer.commit(batch.length);
                lastSendTime.set(System.currentTimeMillis());
                if (e != null) {
                    addError(String.format(
                        "Error while sending Batch #%x (%s records) to Loki (%s)",
                        batchId, batch.length, sender.getUrl()), e);
                }
                else {
                    if (r.status < 200 || r.status > 299)
                        addError(String.format(
                            "Loki responded with non-success status %s on Batch #%x (%s records). Error: %s", 
                            r.status, batchId, batch.length, r.body));
                    else
                        addInfo(String.format(
                            "<<< Batch #%x: Loki responded with status %s",
                            batchId, r.status));
                }
            })
            .thenApply(r -> null);
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        buffer.waitForEmpty(timeoutMs);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBatchTimeoutMs() {
        return batchTimeoutMs;
    }
    public void setBatchTimeoutMs(long batchTimeoutMs) {
        this.batchTimeoutMs = batchTimeoutMs;
    }
    public void setSendQueueSize(int sendQueueSize) {
        this.sendQueueSize = sendQueueSize;
    }

    /**
     * "format" instead of "encoder" in the name allows to specify
     * the default implementation, so users don't have to write
     * full-qualified class name by default
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
     * for the configuration section
     */
    @DefaultClass(JavaHttpSender.class)
    public void setHttp(HttpSender sender) {
        this.sender = sender;
    }

    public void setProcessingThreads(int processingThreads) {
        this.processingThreads = processingThreads;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

}
