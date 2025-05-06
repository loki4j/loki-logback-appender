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

    private static final Comparator<LogRecord> compareByStream = (e1, e2) ->
        Long.compare(e1.stream.hashCode(), e2.stream.hashCode());

    private final ConcurrentLinkedQueue<LogRecord> buffer = new ConcurrentLinkedQueue<>();

    private final long parkTimeoutNs;

    private final ByteBufferQueue sendQueue;

    private final Batcher batcher;

    private final Optional<Comparator<LogRecord>> recordComparator;

    private final Writer writer;

    /**
     * A HTTP client to use for pushing logs to Loki
     */
    private final Loki4jHttpClient httpClient;

    /**
     * A tracker for the performance metrics (if enabled)
     */
    private final Loki4jMetrics metrics;

    private final Loki4jLogger log;

    private final ExponentialBackoff backoffMs;

    private final Jitter jitterMs;

    private final boolean drainOnStop;

    private final int maxRetries;

    /**
     * Disables retries of batches that Loki responds to with a 429 status code (TooManyRequests).
     * This reduces impacts on batches from other tenants, which could end up being delayed or dropped
     * due to backoff.
     */
    private final boolean dropRateLimitedBatches;

    private volatile boolean started = false;

    private AtomicBoolean acceptNewEvents = new AtomicBoolean(true);

    private AtomicBoolean drainRequested = new AtomicBoolean(false);

    private AtomicLong lastSendTimeMs = new AtomicLong(System.currentTimeMillis());

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

        drainScheduledFuture.cancel(false);

        if (drainOnStop) {
            log.info("Pipeline is draining...");
            waitSendQueueLessThan(batcher.getCapacity(), Long.MAX_VALUE);
            lastSendTimeMs.set(0);
            drain();
            waitSendQueueIsEmpty(Long.MAX_VALUE);
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

    public void waitSendQueueIsEmpty(long timeoutMs) {
        waitSendQueueLessThan(1, timeoutMs);
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
                encodeStep(batch);
            } catch (InterruptedException e) {
                stop();
            }
        }
    }

    private void runSendLoop() {
        while (started) {
            try {
                sendStep();
            } catch (InterruptedException e) {
                stop();
            }
        }
    }

    private void encodeStep(LogRecordBatch batch) throws InterruptedException {
        while (started && buffer.isEmpty() && !drainRequested.get()) {
            LockSupport.parkNanos(this, parkTimeoutNs);
        }
        if (!started) return;
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
        drainRequested.set(false);
        if (batch.isEmpty()) return;

        writeBatch(batch, writer);
        if (writer.isEmpty()) return;
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

    private void sendStep() throws InterruptedException {
        BinaryBatch batch = sendQueue.borrowBuffer();
        while(started && batch == null) {
            LockSupport.parkNanos(this, parkTimeoutNs);
            batch = sendQueue.borrowBuffer();
        }
        if (!started) return;
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
            log.error(e,
                "%sError while sending Batch %s to Loki (%s)",
                isRetry ? "Retry #" + retry + ". " : "", batch, httpClient.getConfig().pushUrl);
        } else {
            log.error(
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

    void waitSendQueueLessThan(int size, long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started && unsentEvents.get() >= size && elapsedNs < timeoutNs) {
            LockSupport.parkNanos(parkTimeoutNs);
            elapsedNs += parkTimeoutNs;
        }
        log.trace("Wait send queue: started=%s, buffer(%s)>=%s, %s ms %s elapsed",
                started, unsentEvents.get(), size, timeoutMs, elapsedNs < timeoutNs ? "not" : "");
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

}
