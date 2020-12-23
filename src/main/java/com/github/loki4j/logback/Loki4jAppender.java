package com.github.loki4j.logback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.common.ConcurrentBatchBuffer;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.ReflectionUtils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.status.Status;

/**
 * Class that provides basic Loki4j functionality for sending log record batches
 */
public class Loki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final LogRecord[] ZERO_EVENTS = new LogRecord[0];

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
     * If true, appender will pring its own debug logs to stderr
     */
    private boolean verbose = false;

    /**
     * An encoder to use for converting log record batches to format acceptable by Loki
     */
    private Loki4jEncoder encoder = new JsonEncoder();

    /**
     * A HTTPS sender to use for pushing logs to Loki
     */
    private AbstractHttpSender sender = ReflectionUtils
        .<AbstractHttpSender>tryCreateInstance("com.github.loki4j.logback.JavaHttpSender")
        .orElse(null);

    private ConcurrentBatchBuffer<ILoggingEvent, LogRecord> buffer;

    private ScheduledExecutorService scheduler;


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

        encoder.setContext(context);
        encoder.start();

        buffer = new ConcurrentBatchBuffer<>(batchSize, LogRecord::create, (e, r) -> encoder.eventToRecord(e, r));

        scheduler = Executors.newScheduledThreadPool(processingThreads, new LokiThreadFactory("loki-scheduler"));

        if (sender == null) {
            // only possible on Java 8
            new RuntimeException("No sender specified. Please specify a sender explicitly in logback config");
        }
        sender.setContext(context);
        sender.setContentType(encoder.getContentType());
        sender.start();

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
        

        sender.stop();
        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        appendAsync(event);
    }

    protected final CompletableFuture<Void> appendAsync(ILoggingEvent event) {
        try {
            event.prepareForDeferredProcessing();
        } catch (RuntimeException e) {
            addWarn("Unable to prepare the event for deferred processing", e);
        }

        var batch = buffer.add(event, ZERO_EVENTS);
        if (batch.length > 0)
            return handleBatchAsync(batch).thenApply(r -> null);
        else
            return CompletableFuture.completedFuture(null);
    }


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
            .thenCompose(sender::sendAsync)
            .whenComplete((r, e) -> {
                if (e != null) {
                    addError(String.format(
                        "Error while sending Batch #%x (%s records) to Loki (%s)",
                        batchId, batch.length, sender.url), e);
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

    AbstractHttpSender getSender() {
        return sender;
    }

    public void setSender(AbstractHttpSender sender) {
        this.sender = sender;
    }

    public void setProcessingThreads(int processingThreads) {
        this.processingThreads = processingThreads;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
