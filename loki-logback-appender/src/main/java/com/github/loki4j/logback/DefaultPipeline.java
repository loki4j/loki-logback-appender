package com.github.loki4j.logback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.loki4j.common.Batcher;
import com.github.loki4j.common.BinaryBatch;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;
import com.github.loki4j.common.SoftLimitBuffer;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

public final class DefaultPipeline extends ContextAwareBase implements LifeCycle {

    private static final LogRecord[] ZERO_RECORDS = new LogRecord[0];

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(1);

    private final SoftLimitBuffer<LogRecord> buffer;

    private final ArrayBlockingQueue<BinaryBatch> senderQueue = new ArrayBlockingQueue<>(10);

    private final Batcher batcher;

    private ScheduledExecutorService scheduler;
    private ExecutorService encoderThreadPool;
    private ExecutorService senderThreadPool;

    private final Function<LogRecordBatch, BinaryBatch> encode;

    private final Function<BinaryBatch, LokiResponse> send;

    private volatile boolean started = false;

    private AtomicBoolean drainRequested = new AtomicBoolean(false);

    private AtomicLong lastSendTimeMs = new AtomicLong(System.currentTimeMillis());

    public DefaultPipeline(
            SoftLimitBuffer<LogRecord> buffer,
            Batcher batcher,
            Function<LogRecordBatch, BinaryBatch> encode,
            Function<BinaryBatch, LokiResponse> send) {
        this.buffer = buffer;
        this.batcher = batcher;
        this.encode = encode;
        this.send = send;
    }

    @Override
    public void start() {
        addInfo("Pipeline is starting...");

        started = true;

        senderThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki-sender"));
        senderThreadPool.execute(() -> runSendLoop());

        encoderThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki-encoder"));
        encoderThreadPool.execute(() -> runEncodeLoop());

        scheduler = Executors.newScheduledThreadPool(1, new LokiThreadFactory("loki-scheduler"));
        scheduler.scheduleAtFixedRate(
            () -> drain(),
            100,
            100,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        addInfo("Pipeline is stopping...");

        waitSendQueueLessThan(batcher.getCapacity(), 1000);
        lastSendTimeMs.set(0);
        drain();
        waitSendQueueIsEmpty(100);

        started = false;

        scheduler.shutdown();
        encoderThreadPool.shutdown();
        senderThreadPool.shutdown();

        try {
            scheduler.awaitTermination(500, TimeUnit.MILLISECONDS);
            encoderThreadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
            senderThreadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            addInfo("Pipeline was interrupted while stopping");
        }
    }

    public boolean append(Supplier<LogRecord> event) {
        return buffer.offer(event);
    }

    private void drain() {
        drainRequested.set(true);
    }

    private void runEncodeLoop() {
        var batch = new LogRecordBatch();
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
        while (started &&
                (noEncodeActions() || senderQueue.remainingCapacity() == 0)) {
            LockSupport.parkNanos(this, PARK_NS);
        }
        if (!started) return;

        LogRecord[] records = ZERO_RECORDS;
        LogRecord record = buffer.poll();
        while(record != null && records.length == 0) {
            records = batcher.add(record, ZERO_RECORDS);
            if (records.length == 0) record = buffer.poll();
        }
        if (records.length == 0 && drainRequested.get()) {
            records = batcher.drain(lastSendTimeMs.get(), ZERO_RECORDS);
            //System.out.println("drained items: " + records.length);
        }
        drainRequested.set(false);
        if (records.length == 0) return;

        batch.init(records);
        var binBatch = encode.apply(batch);
        var sent = false;
        while(started && !sent)
            sent = senderQueue.offer(binBatch, PARK_NS, TimeUnit.NANOSECONDS);
    }

    private boolean noEncodeActions() {
        return buffer.isEmpty() && !drainRequested.get();
    }

    private void sendStep() throws InterruptedException {
        BinaryBatch batch = null;
        while(started && batch == null) {
            if (senderQueue.size() > 0)
                batch = senderQueue.poll(PARK_NS, TimeUnit.NANOSECONDS);
            else
                LockSupport.parkNanos(this, PARK_NS);
        }
        if (!started) return;

        // TODO: handle exceptions?
        send.apply(batch);

        buffer.commit(batch.recordsCount);
        lastSendTimeMs.set(System.currentTimeMillis());
        //System.out.println("sent items: " + batch.recordsCount);
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        waitSendQueueLessThan(1, timeoutMs);
    }

    void waitSendQueueLessThan(int size, long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started && buffer.size() >= size && elapsedNs < timeoutNs) {
            LockSupport.parkNanos(PARK_NS);
            elapsedNs += PARK_NS;
        }
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

    @Override
    public boolean isStarted() {
        return started;
    }

}
