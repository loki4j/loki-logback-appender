package com.github.loki4j.logback;

import com.github.loki4j.common.Batcher;
import com.github.loki4j.common.BinaryBatch;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.SoftLimitBuffer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.status.Status;

/**
 * Main appender that provides functionality for sending log record batches to Loki
 */
public class Loki4jAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

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

    private DefaultPipeline pipeline;

    @Override
    public void start() {
        if (getStatusManager() != null && getStatusManager().getCopyOfStatusListenerList().isEmpty()) {
            var statusListener = new StatusPrinter(verbose ? Status.INFO : Status.WARN);
            statusListener.setContext(getContext());
            statusListener.start();
            getStatusManager().add(statusListener);
        }

        addInfo(String.format("Starting with " +
            "batchSize=%s, batchTimeout=%s...",
            batchSize, batchTimeoutMs));

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

        if (sender == null) {
            addWarn("No sender specified in the config. Trying to use JavaHttpSender with default settings");
            sender = new JavaHttpSender();
        }
        sender.setContext(context);
        sender.setContentType(encoder.getContentType());
        sender.start();

        var buffer = new SoftLimitBuffer<LogRecord>(sendQueueSize);
        var batcher = new Batcher(batchSize, batchTimeoutMs);
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
        if (metricsEnabled)
            metrics.eventAppended(startedNs, !appended);
    }

    protected BinaryBatch encode(LogRecordBatch batch) {
        var startedNs = System.nanoTime();
        var encoded = encoder.encode(batch.records);
        var binBatch = BinaryBatch.fromLogRecordBatch(batch, encoded);
        addInfo(String.format(
            ">>> Batch %s converted to %,d bytes",
                batch, binBatch.data.length));
        //try { System.out.write(body); } catch (Exception e) { e.printStackTrace(); }
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

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
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

}
