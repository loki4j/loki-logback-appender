package com.github.loki4j.client.pipeline;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import com.github.loki4j.client.batch.Batcher;
import com.github.loki4j.client.batch.BinaryBatch;
import com.github.loki4j.client.batch.ByteBufferQueue;
import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.http.HttpStatus;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.http.LokiResponse;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.client.util.Loki4jLogger;
import com.github.loki4j.client.util.Loki4jThreadFactory;
import com.github.loki4j.client.writer.Writer;

import static com.github.loki4j.client.util.StringUtils.bytesAsBase64String;
import static com.github.loki4j.client.util.StringUtils.bytesAsUtf8String;

public final class AsyncBufferPipeline {

    /**
     * Comparator used for sorting the bath, i.e., to group records by stream
     */
    private static final Comparator<LogRecord> compareByStream = (e1, e2) ->
        Long.compare(e1.stream.hashCode(), e2.stream.hashCode());

    /**
     * Thread-safe buffer to store incoming log records before they are batched (append -> encode)
     */
    private final ConcurrentLinkedQueue<LogRecord> buffer = new ConcurrentLinkedQueue<>();

    /**
     * Configurable timeout to park a thread for during one iteration of waiting
     */
    private final long parkTimeoutNs;

    /**
     * A queue for outgoing encoded batches (encode -> send)
     */
    private final ByteBufferQueue sendQueue;

    /**
     * Batcher is responsible for accumulating log records according to configured batching rules.
     * It is used on encoding step
     */
    private final Batcher batcher;

    /**
     * Used on encode step to order log records in the batch before serializing them
     */
    private final Optional<Comparator<LogRecord>> recordComparator;

    /**
     * Used on encode step to serialize (encode) log records into binary data
     */
    private final Writer writer;

    /**
     * HTTP client to use for pushing logs to Loki
     */
    private final Loki4jHttpClient httpClient;

    /**
     * A tracker for the performance metrics (if enabled)
     */
    private final Loki4jMetrics metrics;

    private final Loki4jLogger log;

    private final ExponentialBackoff backoffMs;

    private final Jitter jitterMs;

    /**
     * See {@link PipelineConfig#drainOnStop}
     */
    private final boolean drainOnStop;
    /**
     * See {@link PipelineConfig#maxRetries}
     */
    private final int maxRetries;

    /**
     * Disables retries of batches that Loki responds to with a 429 status code (TooManyRequests).
     * This reduces impacts on batches from other tenants, which could end up being delayed or dropped
     * due to backoff.
     */
    private final boolean dropRateLimitedBatches;

    /**
     * This flag, if false, terminates the event loops: encode loop and send loop
     */
    private volatile boolean started = false;

    /**
     * This flag is set to true when the {@link #stop()} method is called.
     * It is used to suppress logging errors to prevent some weird frameworks (Spring) from failing
     */
    private volatile boolean isStopping = false;

    /**
     * This flag is true when encode step is running.
     * It is used in {@link #waitPipelineIsEmpty(long)} to track if encoding is still running while the {@link buffer} is empty
     */
    private volatile boolean isEncodeRunning = false;
    /**
     * This flag is true when send step is running.
     * It is used in {@link #waitPipelineIsEmpty(long)} to track if sending is still running while the {@link #sendQueue} is empty
     */
    private volatile boolean isSendRunning = false;

    /**
     * When {@link #sendQueue} is full and unable to accept more batches, this flag is set to false.
     * This signals the append step to stop accepting new log records, they are dropped instead.
     * <p>
     * This is a backpressure mechanism between send and append to prevent OOMs when too many records got stuck in buffers
     */
    private AtomicBoolean acceptNewEvents = new AtomicBoolean(true);

    /**
     * Periodically is set to true by {@link #drainScheduledFuture} to check whether a batch should be cut
     * by time elapsed since the last send (tracked in {@link #lastSendTimeMs}).
     * <p>
     * When {@link #stop()} is called, depending on {@link #drainOnStop} a drain can also be requested.
     * <p>
     * Drain means the batch is send as is, even if it's not fully packed with log records.
     * This flag is used on encode step
     */
    private AtomicBoolean drainRequested = new AtomicBoolean(false);

    /**
     * Tracks when the last send operation was completed.
     * It is used for draining on encode step
     */
    private AtomicLong lastSendTimeMs = new AtomicLong(System.currentTimeMillis());

    /**
     * This is used for metrics only.
     */
    private AtomicLong unsentEvents = new AtomicLong(0L);

    private ScheduledExecutorService scheduler;
    private ExecutorService encoderThreadPool;
    private ExecutorService senderThreadPool;

    private ScheduledFuture<?> drainScheduledFuture;

    public AsyncBufferPipeline(PipelineConfig conf) {
        Optional<Comparator<LogRecord>> logRecordComparator = conf.staticLabels
            ? Optional.empty()
            : Optional.of(compareByStream);

        ByteBufferFactory bufferFactory = new ByteBufferFactory(conf.useDirectBuffers);

        batcher = new Batcher(conf.batchMaxItems, conf.batchMaxBytes, conf.batchTimeoutMs);
        recordComparator = logRecordComparator;
        writer = conf.writerFactory.factory.apply(conf.batchMaxBytes, bufferFactory);
        sendQueue = new ByteBufferQueue(conf.sendQueueMaxBytes, bufferFactory);
        httpClient = conf.httpClientFactory.apply(conf.httpConfig);
        backoffMs = new ExponentialBackoff(conf.minRetryBackoffMs, conf.maxRetryBackoffMs);
        jitterMs = new Jitter(conf.maxRetryJitterMs);
        drainOnStop = conf.drainOnStop;
        maxRetries = conf.maxRetries;
        dropRateLimitedBatches = conf.dropRateLimitedBatches;
        parkTimeoutNs = TimeUnit.MILLISECONDS.toNanos(conf.internalQueuesCheckTimeoutMs);
        this.log = conf.internalLoggingFactory.apply(this);
        this.metrics = conf.metricsEnabled ? new Loki4jMetrics(conf.name, () -> unsentEvents.get()) : null;
    }

    public void start() {
        log.info("Pipeline is starting...");

        isStopping = false;
        started = true;

        senderThreadPool = Executors.newFixedThreadPool(1, new Loki4jThreadFactory("loki4j-sender"));
        senderThreadPool.execute(() -> runSendLoop());

        encoderThreadPool = Executors.newFixedThreadPool(1, new Loki4jThreadFactory("loki4j-encoder"));
        encoderThreadPool.execute(() -> runEncodeLoop());

        scheduler = Executors.newScheduledThreadPool(1, new Loki4jThreadFactory("loki4j-scheduler"));
        drainScheduledFuture = scheduler.scheduleAtFixedRate(
            () -> drain(),
            100,
            100,
            TimeUnit.MILLISECONDS);

        log.trace("Pipeline started");
    }

    public void stop() {
        log.trace("Pipeline is stopping...");

        isStopping = true;

        drainScheduledFuture.cancel(false);

        if (drainOnStop) {
            log.info("Pipeline is draining...");
            waitPipelineIsEmpty(Long.MAX_VALUE);
            lastSendTimeMs.set(0);
            drain();
            waitPipelineIsEmpty(Long.MAX_VALUE);
            log.info("Drain completed");
        }

        started = false;

        scheduler.shutdown();
        encoderThreadPool.shutdown();
        senderThreadPool.shutdown();

        try {
            httpClient.close();
        } catch (Exception e) {
            log.error(e, "Error while closing HttpClient");
        }

        log.trace("Pipeline stopped");
    }

    public void waitPipelineIsEmpty(long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started
                && (drainRequested.get() || !buffer.isEmpty() || isEncodeRunning || !sendQueue.isEmpty() || isSendRunning)
                && elapsedNs < timeoutNs) {
            LockSupport.parkNanos(parkTimeoutNs);
            elapsedNs += parkTimeoutNs;
        }
        log.trace("Wait send queue: started=%s, unsent=%s, %s ms %s elapsed",
                started, unsentEvents.get(), timeoutMs, elapsedNs < timeoutNs ? "not" : "");
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

    public boolean append(Supplier<LogRecord> recordSupplier) {
        var startedNs = System.nanoTime();
        boolean accepted = false;
        if (acceptNewEvents.get()) {
            LogRecord record = null;
            try {
                record = recordSupplier.get();
            } catch (Exception e) {
                log.error(e, "Error occurred while appending an event");
                if (metrics != null) metrics.appendFailed(() -> e.getClass().getSimpleName());
                accepted = true;
            }
            if (record != null && batcher.validateLogRecordSize(record)) {
                buffer.offer(record);
                unsentEvents.incrementAndGet();
                accepted = true;
                log.trace("Log record was accepted for sending: %s", record);
            } else if (record != null) {
                log.warn("Dropping the record that exceeds max batch size: %s", record);
            }
        }
        if (metrics != null)
            metrics.eventAppended(startedNs, !accepted);
        return accepted;
    }

    private void drain() {
        drainRequested.set(true);
        log.trace("Drain planned");
    }

    private void runEncodeLoop() {
        var batch = new LogRecordBatch(batcher.getCapacity());
        while (started) {
            try {
                while (started && buffer.isEmpty() && !drainRequested.get()) {
                    LockSupport.parkNanos(this, parkTimeoutNs);
                }
                if (!started) return;
                isEncodeRunning = true;
                encodeStep(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                isEncodeRunning = false;
            }
        }
    }

    private void runSendLoop() {
        while (started) {
            try {
                BinaryBatch batch = sendQueue.borrowBuffer();
                while(started && batch == null) {
                    LockSupport.parkNanos(this, parkTimeoutNs);
                    batch = sendQueue.borrowBuffer();
                }
                if (!started) return;
                isSendRunning = true;
                sendStep(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                isSendRunning = false;
            }
        }
    }

    private void encodeStep(LogRecordBatch batch) throws InterruptedException {
        log.trace("Checking encode actions...");
        LogRecord record = buffer.peek();
        while(record != null && batch.isEmpty()) {
            batcher.checkSizeBeforeAdd(record, batch);
            if (batch.isEmpty()) batcher.add(buffer.remove(), batch);
            if (batch.isEmpty()) record = buffer.peek();
        }

        if (batch.isEmpty() && drainRequested.get()) {
            batcher.drain(lastSendTimeMs.get(), batch);
            log.trace("Draining %s remained log records for encode", batch.size());
        }
        if (batch.isEmpty()) {
            drainRequested.set(false);
            return;
        }

        writeBatch(batch, writer);
        if (writer.isEmpty()) {
            drainRequested.set(false);
            return;
        }
        while(started &&
                !sendQueue.offer(
                    batch.batchId(),
                    batch.size(),
                    writer.size(),
                    b -> writer.toByteBuffer(b))) {
            acceptNewEvents.set(false);
            LockSupport.parkNanos(this, parkTimeoutNs);
        }
        batch.clear();
        acceptNewEvents.set(true);
        drainRequested.set(false);
    }

    private void writeBatch(LogRecordBatch batch, Writer writer) {
        var startedNs = System.nanoTime();
        recordComparator.ifPresent(cmp -> batch.sort(cmp));
        try {
            writer.serializeBatch(batch);
            log.info(
                ">>> Batch %s converted to %,d bytes",
                    batch, writer.size());
            if (metrics != null)
                metrics.batchEncoded(startedNs, writer.size());
        } catch (Exception e) {
            log.error(e, "Error occurred while serializing batch %s", batch);
            unsentEvents.addAndGet(-batch.size());
            if (metrics != null) metrics.batchEncodeFailed(() -> e.getClass().getSimpleName());
            writer.reset();
            batch.clear();
        }
    }

    private void sendStep(BinaryBatch batch) throws InterruptedException {
        try {
            sendBatch(batch);
            lastSendTimeMs.set(System.currentTimeMillis());
            log.trace("Batch %s was successfully sent to Loki", batch);
        } finally {
            unsentEvents.addAndGet(-batch.sizeItems);
            sendQueue.returnBuffer(batch);
        }
    }

    private LokiResponse sendBatch(BinaryBatch batch) {
        var startedNs = System.nanoTime();
        LokiResponse r = null;
        Exception e = null;
        int retry = 0;

        do {
            batch.data.rewind();
            // print out the batch before send if tracing is enabled
            if (log.isTraceEnabled(this)) {
                var payload = new byte[batch.data.limit()];
                batch.data.get(payload);
                batch.data.rewind();
                log.trace("Sending batch %s with %spayload:\n%s",
                    batch,
                    writer.isBinary() ? "binary " : "",
                    writer.isBinary() ? bytesAsBase64String(payload) : bytesAsUtf8String(payload));
            }
            // try to send the batch
            try {
                r = httpClient.send(batch.data);
                // exit if send is successful
                if (r.status >= 200 && r.status < 300) {
                    log.info("<<< %sBatch %s: Loki responded with status %s",
                        retry > 0 ? "Retry #" + retry + ". " : "", batch, r.status);
                    if (metrics != null) metrics.batchSent(startedNs, batch.sizeBytes);
                    return r;
                }
            } catch (Exception re) {
                e = re;
            }
            reportSendError(batch, e, r, retry);
        } while (
            ++retry <= maxRetries
            && checkIfEligibleForRetry(e, r)
            && reportRetryFailed(e, r)
            && backoffSleep(retry));

        if (metrics != null) metrics.batchSendFailed(sendErrorReasonProvider(e, r));
        return null;
    }

    private void reportSendError(BinaryBatch batch, Exception e, LokiResponse r, int retry) {
        // whether exception occurred or error status received
        var exceptionOccurred = e != null;
        var isRetry = retry > 0;

        if (exceptionOccurred) {
            log.errorOrWarn(!isStopping, e,
                "%sError while sending Batch %s to Loki (%s)",
                isRetry ? "Retry #" + retry + ". " : "", batch, httpClient.getConfig().pushUrl);
        } else {
            log.errorOrWarn(!isStopping, e,
                "%sLoki responded with non-success status %s on batch %s. Error: %s",
                isRetry ? "Retry #" + retry + ". " : "", r.status, batch, r.body);
        }
    }

    private boolean reportRetryFailed(Exception e, LokiResponse r) {
        if (metrics != null) metrics.sendRetryFailed(sendErrorReasonProvider(e, r));
        return true;
    }

    private Supplier<String> sendErrorReasonProvider(Exception e, LokiResponse r) {
        return () ->
            e != null
                ? "exception:" + e.getClass().getSimpleName()
                : "status:" + r.status;
    }

    private boolean checkIfEligibleForRetry(Exception e, LokiResponse r) {
        return e instanceof ConnectException
                || e instanceof HttpTimeoutException
                || (r != null && checkIfStatusEligibleForRetry(r.status));
    }

    private boolean checkIfStatusEligibleForRetry(int status) {
        return status == HttpStatus.SERVICE_UNAVAILABLE
                || (status == HttpStatus.TOO_MANY_REQUESTS && !dropRateLimitedBatches);
    }

    private boolean backoffSleep(int retryNo) {
        if (retryNo == 1)
            backoffMs.reset();    // resetting backoff state on first retry
        var timeoutMs = backoffMs.nextDelay() + jitterMs.nextJitter();
        log.trace("Retry #%s backoff timeout: %s ms; state: %s", retryNo, timeoutMs, backoffMs);
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

}
