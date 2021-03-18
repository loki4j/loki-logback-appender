package com.github.loki4j.logback;

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

import com.github.loki4j.common.Batcher;
import com.github.loki4j.common.BinaryBatch;
import com.github.loki4j.common.ByteBufferQueue;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LogRecordStream;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;
import com.github.loki4j.common.Writer;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

public final class DefaultPipeline extends ContextAwareBase implements LifeCycle {

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(1);

    private final ConcurrentLinkedQueue<LogRecord> buffer = new ConcurrentLinkedQueue<>();

    private final ByteBufferQueue senderQueue;

    private final Batcher batcher;

    private Optional<Comparator<LogRecord>> recordComparator;

    private final Writer writer;

    /**
     * A HTTPS sender to use for pushing logs to Loki
     */
    private final HttpSender sender;

    /**
     * A tracker for the appender's metrics (if enabled)
     */
    private final LoggerMetrics metrics;

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

    private boolean traceEnabled = false;

    public DefaultPipeline(
            Batcher batcher,
            Optional<Comparator<LogRecord>> recordComparator,
            Writer writer,
            ByteBufferQueue senderQueue,
            HttpSender sender,
            LoggerMetrics metrics,
            boolean drainOnStop) {
        this.batcher = batcher;
        this.recordComparator = recordComparator;
        this.writer = writer;
        this.senderQueue = senderQueue;
        this.sender = sender;
        this.drainOnStop = drainOnStop;
        this.metrics = metrics;
    }

    @Override
    public void start() {
        addInfo("Pipeline is starting...");

        started = true;

        senderThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki4j-sender"));
        senderThreadPool.execute(() -> runSendLoop());

        encoderThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki4j-encoder"));
        encoderThreadPool.execute(() -> runEncodeLoop());

        scheduler = Executors.newScheduledThreadPool(1, new LokiThreadFactory("loki4j-scheduler"));
        drainScheduledFuture = scheduler.scheduleAtFixedRate(
            () -> drain(),
            100,
            100,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        drainScheduledFuture.cancel(false);

        if (drainOnStop) {
            addInfo("Pipeline is draining...");
            waitSendQueueLessThan(batcher.getCapacity(), Long.MAX_VALUE);
            lastSendTimeMs.set(0);
            drain();
            waitSendQueueIsEmpty(Long.MAX_VALUE);
            addInfo("Drain completed");
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
                addWarn("Dropping the record that exceeds max batch size: " + record.toString());
            }
        }
        if (metrics != null)
            metrics.eventAppended(startedNs, !accepted);
        return accepted;
    }

    private void drain() {
        drainRequested.set(true);
        trace("drain planned");
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
        trace("check encode actions");
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
        while(started && 
                !senderQueue.offer(
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
        writer.serializeBatch(batch);
        addInfo(String.format(
            ">>> Batch %s converted to %,d bytes",
                batch, writer.size()));
        //try { System.out.write(binBatch.data); } catch (Exception e) { e.printStackTrace(); }
        //System.out.println("\n");
        if (metrics != null)
            metrics.batchEncoded(startedNs, writer.size());
    }

    private void sendStep() throws InterruptedException {
        BinaryBatch batch = senderQueue.borrowBuffer();
        while(started && batch == null) {
            LockSupport.parkNanos(this, PARK_NS);
            batch = senderQueue.borrowBuffer();
        }
        if (!started) return;
        try {
            sendBatch(batch);
            lastSendTimeMs.set(System.currentTimeMillis());
            trace("sent items: %s", batch.sizeItems);
        } finally {
            unsentEvents.addAndGet(-batch.sizeItems);
            senderQueue.returnBuffer(batch);
        }
    }

    private LokiResponse sendBatch(BinaryBatch batch) {
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

        if (metrics != null)
            metrics.batchSent(startedNs, batch.sizeBytes, e != null || r.status > 299);

        return r;
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        waitSendQueueLessThan(1, timeoutMs);
    }

    void waitSendQueueLessThan(int size, long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started && unsentEvents.get() >= size && elapsedNs < timeoutNs) {
            LockSupport.parkNanos(PARK_NS);
            elapsedNs += PARK_NS;
        }
        trace("wait send queue: started=%s, buffer(%s)>=%s, %s ms %s elapsed",
                started, unsentEvents.get(), size, timeoutMs, elapsedNs < timeoutNs ? "not" : "");
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

    private void trace(String input, Object... args) {
        if (traceEnabled)
            addInfo(String.format(input, args));
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

}
