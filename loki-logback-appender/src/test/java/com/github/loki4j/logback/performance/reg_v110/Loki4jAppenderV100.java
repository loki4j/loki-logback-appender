package com.github.loki4j.logback.performance.reg_v110;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;
import com.github.loki4j.logback.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki
 */
public class Loki4jAppenderV100 extends UnsynchronizedAppenderBase<ILoggingEvent> {

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
     * If true, appender will print its own debug logs to stderr
     */
    private boolean verbose = false;

    /**
     * An encoder to use for converting log record batches to format acceptable by Loki
     */
    private Loki4jEncoder encoder;

    /**
     * A HTTPS sender to use for pushing logs to Loki
     */
    private HttpSender sender;

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
            "procThreads=%s, batchSize=%s, batchTimeout=%s...",
            processingThreads, batchSize, batchTimeoutMs));

        if (encoder == null) {
            addWarn("No encoder specified in the config. Using JsonEncoder with default settings");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        buffer = new ConcurrentBatchBuffer<>(batchSize, LogRecord::create, (e, r) -> encoder.eventToRecord(e));

        scheduler = Executors.newScheduledThreadPool(processingThreads, new LokiThreadFactory("loki-scheduler"));
        httpThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki-http-sender"));

        if (sender == null) {
            addWarn("No sender specified in the config. Trying to use JavaHttpSender with default settings");
            sender = new JavaHttpSender();
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
        httpThreadPool.shutdown();
        
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

    protected byte[] encode(LogRecord[] batch) {
        return encoder.encode(new LogRecordBatch(batch));
    }

    protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        return CompletableFuture
            .supplyAsync(() -> sender.send(batch), httpThreadPool);
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
                var body = encode(batch);
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

    public static class Wrapper<T extends Loki4jAppenderV100> {
        private T appender;
        public Wrapper(T appender) {
            this.appender = appender;
        }
        public void append(ILoggingEvent event) {
            appender.append(event);
        }
        @SuppressWarnings("unchecked")
        public void appendAndWait(ILoggingEvent... events) {
            var fs = (CompletableFuture<Void>[]) new CompletableFuture[events.length];
            for (int i = 0; i < events.length; i++) {
                fs[i] = appender.appendAsync(events[i]);
            }
            try {
                CompletableFuture.allOf(fs).get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Error while waiting for futures", e);
            }
        }
        public void stop() {
            appender.stop();
        }
    }

}
