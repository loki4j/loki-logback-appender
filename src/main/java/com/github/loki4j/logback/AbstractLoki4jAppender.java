package com.github.loki4j.logback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.common.ConcurrentBatchBuffer;
import com.github.loki4j.common.LogRecord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.status.Status;

/**
 * Abstract class that provides basic Loki4j functionality for sending log record batches
 */
public abstract class AbstractLoki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final LogRecord[] ZERO_EVENTS = new LogRecord[0];

    static final class LokiResponse {
        public int status;
        public String body;
        public LokiResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /**
     * Loki endpoint to be used for sending batches
     */
    protected String url = "http://localhost:3100/loki/api/v1/push";

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    protected long connectionTimeoutMs = 30_000;
    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    protected long requestTimeoutMs = 5_000;

    /**
     * Max number of messages to put into single batch and send to Loki
     */
    private int batchSize = 1000;
    /**
     * Max time in milliseconds to wait before sending a batch to Loki
     */
    private long batchTimeoutMs = 60 * 1000;

    /**
     * Number of threads to use for log message processing and formatting
     */
    private int processingThreads = 1;
    /**
     * Number of threads to use for sending HTTP requests
     */
    private int httpThreads = 1;

    /**
     * If true, appender will pring its own debug logs to stderr
     */
    private boolean verbose = false;

    /**
     * An encoder to use for converting log record batches to format acceptable by Loki
     */
    private Loki4jEncoder encoder;

    private ConcurrentBatchBuffer<ILoggingEvent, LogRecord> buffer;

    private ScheduledExecutorService scheduler;
    protected ExecutorService httpThreadPool;

    @Override
    public final void start() {
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
            addWarn("No encoder specified. Switching to default encoder");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        buffer = new ConcurrentBatchBuffer<>(batchSize, LogRecord::create, (e, r) -> encoder.eventToRecord(e, r));

        scheduler = Executors.newScheduledThreadPool(processingThreads, new LokiThreadFactory("loki-scheduler"));
        httpThreadPool = Executors.newFixedThreadPool(httpThreads, new LokiThreadFactory("loki-http-sender"));

        startHttp(encoder.getContentType());

        super.start();

        scheduler.scheduleAtFixedRate(
            () -> drainAsync(batchTimeoutMs - 20), // decreasing timeout, so it's slightly less than elapsed time
            100,
            batchTimeoutMs,
            TimeUnit.MILLISECONDS);
        addInfo("Successfully started");
    }

    @Override
    public final void stop() {
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
        httpThreadPool.shutdown();

        stopHttp();
        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            event.prepareForDeferredProcessing();
        } catch (RuntimeException e) {
            addWarn("Unable to prepare the event for deferred processing", e);
        }

        var batch = buffer.add(event, ZERO_EVENTS);
        if (batch.length > 0)
            handleBatchAsync(batch);
    }

    protected abstract void startHttp(String contentType);

    protected abstract void stopHttp();

    protected abstract CompletableFuture<LokiResponse> sendAsync(byte[] batch);
    

    private CompletableFuture<Void> drainAsync(long timeoutMs) {
        var batch = buffer.drain(timeoutMs, ZERO_EVENTS);
        if (batch.length > 0)
            return handleBatchAsync(batch).thenApply(r -> null);
        else
            return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<LokiResponse> handleBatchAsync(LogRecord[] batch) {
        var batchId = System.nanoTime();
        return CompletableFuture
            .supplyAsync(() -> {
                var body = encoder.encode(batch);
                addInfo(String.format(
                    ">>> Batch #%x: Sending %,d items converted to %,d bytes",
                    batchId, batch.length, body.length));
                //try { System.out.write(body); } catch (Exception e) { e.printStackTrace(); }
                //System.out.println("\n");
                return body;
            }, scheduler)
            .thenCompose(this::sendAsync)
            .whenComplete((r, e) -> {
                if (e != null) {
                    addError(String.format(
                        "Error while sending Batch #%x (%s records) to Loki (%s)",
                        batchId, batch.length, url), e);
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
            });
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
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

    public void setEncoder(Loki4jEncoder encoder) {
        this.encoder = encoder;
    }

    public void setProcessingThreads(int processingThreads) {
        this.processingThreads = processingThreads;
    }

    public int getHttpThreads() {
        return httpThreads;
    }
    public void setHttpThreads(int httpThreads) {
        this.httpThreads = httpThreads;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
}
