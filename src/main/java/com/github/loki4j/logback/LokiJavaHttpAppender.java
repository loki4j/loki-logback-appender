package com.github.loki4j.logback;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
 * Loki appender that is backed by Java standard {@link java.net.http.HttpClient HttpClient}
 */
public class LokiJavaHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final LogRecord[] ZERO_EVENTS = new LogRecord[0];

    /**
     * Loki endpoint to be used for sending batches
     */
    private String url = "http://localhost:3100/loki/api/v1/push";

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

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ConcurrentBatchBuffer<ILoggingEvent, LogRecord> buffer;

    private ScheduledExecutorService scheduler;
    private ExecutorService httpThreadPool;

    @Override
    public void start() {
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        addInfo(String.format("Starting with " +
            "procThreads=%s , httpThreads=%s, batchSize=%s, batchTimeout=%s...",
            processingThreads, httpThreads, batchSize, batchTimeoutMs));

        if (encoder == null) {
            addWarn("No encoder specified. Switching to default encoder");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        buffer = new ConcurrentBatchBuffer<>(batchSize, LogRecord::create, (e, r) -> encoder.eventToRecord(e, r));

        scheduler = Executors.newScheduledThreadPool(processingThreads, new LokiThreadFactory("loki-scheduler"));
        httpThreadPool = Executors.newFixedThreadPool(httpThreads, new LokiThreadFactory("loki-http-sender"));

        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(connectionTimeoutMs))
            .executor(httpThreadPool)
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(requestTimeoutMs))
            .uri(URI.create(url))
            .header("Content-Type", encoder.getContentType());

        super.start();

        scheduler.scheduleAtFixedRate(() -> {
            var batch = buffer.drain(batchTimeoutMs, ZERO_EVENTS);
            if (batch.length > 0)
                handleBatchAsync(batch);
        }, 100, batchTimeoutMs, TimeUnit.MILLISECONDS);

        addInfo("Successfully started");
    }

    @Override
    public void stop() {
        if (!super.isStarted()) {
            return;
        }
        addInfo("Stoppping...");

        super.stop();

        encoder.stop();

        scheduler.shutdown();
        httpThreadPool.shutdown();
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

    private CompletableFuture<HttpResponse<String>> handleBatchAsync(LogRecord[] batch) {
        var batchId = System.nanoTime();
        return CompletableFuture
            .supplyAsync(() -> {
                var body = encoder.encode(batch);
                addInfo(String.format(
                    ">>> Batch #%x: Sending %,d items converted to %,d bytes",
                    batchId, batch.length, body.length));
                //try { System.out.write(body); } catch (Exception e) { e.printStackTrace(); }
                //System.out.println("\n");
                var request = requestBuilder
                    .copy()
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
                return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            }, scheduler)
            .thenCompose(r -> r)
            .whenComplete((r, e) -> {
                if (e != null) {
                    addError(String.format(
                        "Error while sending Batch #%x (%s records) to Loki",
                        batchId, batch.length), e);
                }
                else {
                    if (r.statusCode() < 200 || r.statusCode() > 299)
                        addError(String.format(
                            "Loki responded with non-success status %s on Batch #%x (%s records). Error: %s", 
                            r.statusCode(), batchId, batch.length, r.body()));
                    else
                        addInfo(String.format(
                            "<<< Batch #%x: Loki responded with status %s",
                            batchId, r.statusCode()));
                }
            });
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setBatchTimeoutMs(long batchTimeoutMs) {
        this.batchTimeoutMs = batchTimeoutMs;
    }

    public void setEncoder(Loki4jEncoder encoder) {
        this.encoder = encoder;
    }

    public int getProcessingThreads() {
        return processingThreads;
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

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


}
