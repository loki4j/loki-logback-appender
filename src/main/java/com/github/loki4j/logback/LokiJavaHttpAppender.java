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

public class LokiJavaHttpAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final LogRecord[] ZERO_EVENTS = new LogRecord[0];

    private String url = "http://localhost:3100/loki/api/v1/push";

    private long connectionTimeoutMs = 30_000;
    private long requestTimeoutMs = 5_000;

    private int processingThreads = 1;
    private int httpThreads = 1;

    private int batchSize = 1000;
    private long batchTimeoutMs = 60 * 1000;

    private boolean verbose = false;

    private JsonEncoder encoder;

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
            .header("Content-Type", "application/json");

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
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

    public JsonEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(JsonEncoder encoder) {
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
