package com.github.loki4j.common.pipeline;

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

import com.github.loki4j.common.batch.Batcher;
import com.github.loki4j.common.batch.BinaryBatch;
import com.github.loki4j.common.batch.ByteBufferQueue;
import com.github.loki4j.common.batch.LogRecord;
import com.github.loki4j.common.batch.LogRecordBatch;
import com.github.loki4j.common.batch.LogRecordStream;
import com.github.loki4j.common.http.Loki4jHttpClient;
import com.github.loki4j.common.http.LokiResponse;
import com.github.loki4j.common.util.ByteBufferFactory;
import com.github.loki4j.common.util.Loki4jLogger;
import com.github.loki4j.common.util.Loki4jThreadFactory;
import com.github.loki4j.common.writer.Writer;

public final class DefaultPipeline {

    private static final Comparator<LogRecord> compareByTime = (e1, e2) -> {
        var tsCmp = Long.compare(e1.timestampMs, e2.timestampMs);
        return tsCmp == 0 ? Integer.compare(e1.nanos, e2.nanos) : tsCmp;
    };

    private static final Comparator<LogRecord> compareByStream = (e1, e2) ->
        Long.compare(e1.stream.id, e2.stream.id);

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(1);

    private final ConcurrentLinkedQueue<LogRecord> buffer = new ConcurrentLinkedQueue<>();

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

    private final boolean drainOnStop;

    private volatile boolean started = false;

    private AtomicBoolean acceptNewEvents = new AtomicBoolean(true);

    private AtomicBoolean drainRequested = new AtomicBoolean(false);

    private AtomicLong lastSendTimeMs = new AtomicLong(System.currentTimeMillis());

    private AtomicLong unsentEvents = new AtomicLong(0L);

    private ScheduledExecutorService scheduler;
    private ExecutorService encoderThreadPool;
    private ExecutorService senderThreadPool;

    private ScheduledFuture<?> drainScheduledFuture;

    public DefaultPipeline(PipelineConfig conf, Loki4jMetrics metrics) {
        Optional<Comparator<LogRecord>> logRecordComparator = Optional.empty();
        if (conf.staticLabels) {
            if (conf.sortByTime)
                logRecordComparator = Optional.of(compareByTime);
        } else {
            logRecordComparator = Optional.of(
                conf.sortByTime ? compareByStream.thenComparing(compareByTime) : compareByStream);
        }
        ByteBufferFactory bufferFactory = new ByteBufferFactory(conf.useDirectBuffers);

        batcher = new Batcher(conf.batchMaxItems, conf.batchMaxBytes, conf.batchTimeoutMs);
        recordComparator = logRecordComparator;
        writer = conf.writerFactory.factory.apply(conf.batchMaxBytes, bufferFactory);
        sendQueue = new ByteBufferQueue(conf.sendQueueMaxBytes, bufferFactory);
        httpClient = conf.httpClientFactory.apply(conf.httpConfig);
        drainOnStop = conf.drainOnStop;
        this.log = conf.internalLoggingFactory.apply(this);
        this.metrics = metrics;
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
    }

    public void stop() {
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
    }

    public boolean append(long timestamp, int nanos, Supplier<LogRecordStream> stream, Supplier<String> message) {
        var startedNs = System.nanoTime();
        boolean accepted = false;
        if (acceptNewEvents.get()) {
            var record = LogRecord.create(timestamp, nanos, stream.get(), message.get());
            if (batcher.validateLogRecordSize(record)) {
                buffer.offer(record);
                unsentEvents.incrementAndGet();
                accepted = true;
            } else {
                log.warn("Dropping the record that exceeds max batch size: " + record.toString());
            }
        }
        if (metrics != null)
            metrics.eventAppended(startedNs, !accepted);
        return accepted;
    }

    private void drain() {
        drainRequested.set(true);
        log.trace("drain planned");
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
            LockSupport.parkNanos(this, PARK_NS);
        }
        if (!started) return;
        log.trace("check encode actions");
        LogRecord record = buffer.peek();
        while(record != null && batch.isEmpty()) {
            batcher.checkSizeBeforeAdd(record, batch);
            if (batch.isEmpty()) batcher.add(buffer.remove(), batch);
            if (batch.isEmpty()) record = buffer.peek();
        }

        if (batch.isEmpty() && drainRequested.get())
            batcher.drain(lastSendTimeMs.get(), batch);
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
            LockSupport.parkNanos(this, PARK_NS);
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
            //try { System.out.write(batch); } catch (Exception e) { e.printStackTrace(); }
            //System.out.println("\n");
            if (metrics != null)
                metrics.batchEncoded(startedNs, writer.size());
        } catch (Exception e) {
            log.error("Error occurred while serializing batch " + batch, e);
            writer.reset();
        }
    }

    private void sendStep() throws InterruptedException {
        BinaryBatch batch = sendQueue.borrowBuffer();
        while(started && batch == null) {
            LockSupport.parkNanos(this, PARK_NS);
            batch = sendQueue.borrowBuffer();
        }
        if (!started) return;
        try {
            sendBatch(batch);
            lastSendTimeMs.set(System.currentTimeMillis());
            log.trace("sent items: %s", batch.sizeItems);
        } finally {
            unsentEvents.addAndGet(-batch.sizeItems);
            sendQueue.returnBuffer(batch);
        }
    }

    private LokiResponse sendBatch(BinaryBatch batch) {
        var startedNs = System.nanoTime();
        LokiResponse r = null;
        Exception e = null;
        try {
            r = httpClient.send(batch.data);
        } catch (Exception re) {
            e = re;
        }

        if (e != null) {
            log.error(e,
                "Error while sending Batch %s to Loki (%s)",
                    batch, httpClient.getConfig().pushUrl);
        }
        else {
            if (r.status < 200 || r.status > 299)
                log.error(
                    "Loki responded with non-success status %s on batch %s. Error: %s",
                    r.status, batch, r.body);
            else
                log.info(
                    "<<< Batch %s: Loki responded with status %s",
                    batch, r.status);
        }

        if (metrics != null)
            metrics.batchSent(startedNs, batch.sizeBytes, e != null || r.status > 299);

        return r;
    }

    public void waitSendQueueIsEmpty(long timeoutMs) {
        waitSendQueueLessThan(1, timeoutMs);
    }

    void waitSendQueueLessThan(int size, long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started && unsentEvents.get() >= size && elapsedNs < timeoutNs) {
            LockSupport.parkNanos(PARK_NS);
            elapsedNs += PARK_NS;
        }
        log.trace("wait send queue: started=%s, buffer(%s)>=%s, %s ms %s elapsed",
                started, unsentEvents.get(), size, timeoutMs, elapsedNs < timeoutNs ? "not" : "");
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

}
