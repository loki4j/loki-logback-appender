package com.github.loki4j.logback;

import java.util.concurrent.atomic.AtomicLong;

import com.github.loki4j.common.Batcher;
import com.github.loki4j.common.BinaryBatch;
import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.SoftLimitBuffer;
import com.github.loki4j.common.Writer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki
 */
public final class Loki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /**
     * Max number of events to put into a single batch and send to Loki
     */
    private int batchSizeItems = 1000;
    /**
     * Max number of bytes a single batch (as encoded by Loki) can contain.
     * This value should not be greater than server.grpc_server_max_recv_msg_size
     * in your Loki config
     */
    private int batchSizeBytes = 4 * 1024 * 1024;
    /**
     * Max time in milliseconds to wait before sending a batch to Loki
     */
    private long batchTimeoutMs = 60 * 1000;

    /**
     * Max number of events to keep in the send queue.
     * When the queue is full, incoming log events are dropped
     */
    private int sendQueueSize = 50 * 1000;

    /**
     * If true, the appender will print its own debug logs to stderr
     */
    private boolean verbose = false;

    /**
     * If true, the appender will report its metrics using Micrometer
     */
    private boolean metricsEnabled = false;

    /**
     * Wait util all remaining events are sent before shutdown the appender
     */
    private boolean drainOnStop = true;

    /**
     * Use off-heap memory for storing intermediate data
     */
    private boolean useDirectBuffers = true;

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

    /**
     * A pipeline that does all the heavy lifting log records processing
     */
    private DefaultPipeline pipeline;

    /**
     * A counter for events dropped due to backpressure
     */
    private AtomicLong droppedEventsCount = new AtomicLong(0L);

    private Writer writer;

    @Override
    public void start() {
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        addInfo(String.format("Starting with " +
            "batchSizeItems=%s, batchSizeBytes=%s, batchTimeout=%s...",
            batchSizeItems, batchSizeBytes, batchTimeoutMs));

        if (encoder == null) {
            addWarn("No encoder specified in the config. Using JsonEncoder with default settings");
            encoder = new JsonEncoder();
        }
        encoder.setContext(context);
        encoder.start();

        var bufferFactory = new ByteBufferFactory(useDirectBuffers);
        writer = encoder.createWriter(batchSizeBytes, bufferFactory);

        if (metricsEnabled) {
            var host = context.getProperty(CoreConstants.HOSTNAME_KEY);
            metrics = new LoggerMetrics(
                this.getName() == null ? "none" : this.getName(),
                host == null ? "unknown" : host);
        }

        if (sender == null) {
            addWarn("No sender specified in the config. Trying to use JavaHttpSender with default settings");
            sender = new JavaHttpSender();
        }
        sender.setContext(context);
        sender.setContentType(encoder.getContentType());
        sender.start();

        var buffer = new SoftLimitBuffer<LogRecord>(sendQueueSize);
        var batcher = new Batcher(batchSizeItems, batchSizeBytes, batchTimeoutMs);
        pipeline = new DefaultPipeline(buffer, batcher, this::encode, this::send, drainOnStop);
        pipeline.setContext(context);
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
        sender.stop();

        addInfo("Successfully stopped");
    }

    @Override
    protected void append(ILoggingEvent event) {
        var startedNs = System.nanoTime();
        var appended = pipeline.append(() -> encoder.eventToRecord(event));
        if (!appended)
            reportDroppedEvents();
        if (metricsEnabled)
            metrics.eventAppended(startedNs, !appended);
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
                "Backpressure: %s messages dropped. Check `sendQueueSize` setting", dropped));
            if (dropped > 1_000_000_000) {
                addWarn(String.format(
                    "Resetting dropped message counter from %s to 0", dropped));
                droppedEventsCount.set(0L);
            }
        }
    }

    protected BinaryBatch encode(LogRecordBatch batch) {
        var startedNs = System.nanoTime();
        encoder.getLogRecordComparator().ifPresent(cmp -> batch.sort(cmp));
        writer.serializeBatch(batch);
        var binBatch = BinaryBatch.fromLogRecordBatch(batch, writer.toByteArray());
        addInfo(String.format(
            ">>> Batch %s converted to %,d bytes",
                batch, binBatch.data.length));
        //try { System.out.write(binBatch.data); } catch (Exception e) { e.printStackTrace(); }
        //System.out.println("\n");
        if (metricsEnabled)
            metrics.batchEncoded(startedNs, binBatch.data.length);
        return binBatch;
    }

    protected LokiResponse send(BinaryBatch batch) {
        var startedNs = System.nanoTime();
        LokiResponse r = null;
        Exception e = null;
        try {
            r = sender.send(batch.data);
        } catch (Exception re) {
            e = re;
        }

        if (e != null) {
            addError(String.format(
                "Error while sending Batch %s to Loki (%s)",
                    batch, sender.getUrl()), e);
        }
        else {
            if (r.status < 200 || r.status > 299)
                addError(String.format(
                    "Loki responded with non-success status %s on batch %s. Error: %s",
                    r.status, batch, r.body));
            else
                addInfo(String.format(
                    "<<< Batch %s: Loki responded with status %s",
                    batch, r.status));
        }

        if (metricsEnabled)
            metrics.batchSent(startedNs, batch.data.length, e != null || r.status > 299);

        return r;
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        pipeline.waitSendQueueIsEmpty(timeoutMs);
    }

    @Deprecated
    public void setBatchSize(int batchSize) {
        addWarn("Property `batchSize` was replaced with `batchSizeItems`. Please fix your configuration");
    }
    public void setBatchSizeItems(int batchSizeItems) {
        this.batchSizeItems = batchSizeItems;
    }
    public void setBatchSizeBytes(int batchSizeBytes) {
        this.batchSizeBytes = batchSizeBytes;
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

}
