package com.github.loki4j.logback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(10);

    private final SoftLimitBuffer<LogRecord> buffer;

    private final AtomicReference<BatchAction> encodeAction = new AtomicReference<>(BatchAction.NONE);

    private final ArrayBlockingQueue<BinaryBatch> senderQueue = new ArrayBlockingQueue<>(10);

    private final Batcher batcher;

    private ScheduledExecutorService scheduler;
    private ExecutorService httpThreadPool;

    private final Function<LogRecordBatch, BinaryBatch> encode;

    private final Function<BinaryBatch, CompletableFuture<LokiResponse>> send;

    private volatile boolean started = false;

    private volatile long lastSendTimeMs = System.currentTimeMillis();

    public DefaultPipeline(
            SoftLimitBuffer<LogRecord> buffer,
            Batcher batcher,
            Function<LogRecordBatch, BinaryBatch> encode,
            Function<BinaryBatch, CompletableFuture<LokiResponse>> send) {
        this.buffer = buffer;
        this.batcher = batcher;
        this.encode = encode;
        this.send = send;
    }

    @Override
    public void start() {
        addInfo("Pipeline is starting...");

        started = true;

        httpThreadPool = Executors.newFixedThreadPool(1, new LokiThreadFactory("loki-http-sender"));
        httpThreadPool.execute(() -> runSendLoop());

        scheduler = Executors.newScheduledThreadPool(2, new LokiThreadFactory("loki-scheduler"));
        scheduler.execute(() -> runEncodeLoop());

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
        lastSendTimeMs = 0;
        drain();
        waitSendQueueIsEmpty(100);

        started = false;

        scheduler.shutdown();
        httpThreadPool.shutdown();

        try {
            scheduler.awaitTermination(500, TimeUnit.MILLISECONDS);
            httpThreadPool.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            addInfo("Pipeline was interrupted while stopping");
        }
    }

    public boolean append(Supplier<LogRecord> event) {
        if (buffer.offer(event)) {
            encodeAction.set(BatchAction.CHECK);
            return true;
        } else {
            return false;
        }
    }

    private void drain() {
        System.out.println("drain");
        encodeAction.compareAndSet(BatchAction.NONE, BatchAction.DRAIN);
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
        var pendingAction = encodeAction.get();
        while (started &&
                (pendingAction == BatchAction.NONE || senderQueue.remainingCapacity() == 0)) {
            LockSupport.parkNanos(this, PARK_NS);
            pendingAction = encodeAction.get();
        }
        if (!started) return;

        LogRecord[] records = ZERO_RECORDS;
        LogRecord record = buffer.poll();
        while(record != null && records.length == 0) {
            records = batcher.add(record, ZERO_RECORDS);
            if (records.length == 0) record = buffer.poll();
        }
        if (records.length == 0 && pendingAction == BatchAction.DRAIN)
            records = batcher.drain(lastSendTimeMs, ZERO_RECORDS);
        encodeAction.set(BatchAction.NONE);
        if (records.length == 0) return;

        batch.init(records);
        var binBatch = encode.apply(batch);
        var sent = false;
        while(started && !sent)
            sent = senderQueue.offer(binBatch, PARK_NS, TimeUnit.NANOSECONDS);
    }

    private void sendStep() throws InterruptedException {
        BinaryBatch batch = null;
        System.out.println("batch: " + batch);
        while(started && batch == null) {
            batch = senderQueue.poll(PARK_NS, TimeUnit.NANOSECONDS);
            System.out.println("batch: " + batch);
        }
        if (!started) return;

        final var batchToSend = batch;
        send
            .apply(batchToSend)
            .whenComplete((r, e) -> {
                buffer.commit(batchToSend.recordsCount);
                lastSendTimeMs = System.currentTimeMillis();
            });
    }

    void waitSendQueueIsEmpty(long timeoutMs) {
        waitSendQueueLessThan(1, timeoutMs);
    }

    void waitSendQueueLessThan(int size, long timeoutMs) {
        var timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        var elapsedNs = 0L;
        while(started && buffer.size() >= size && elapsedNs < timeoutNs) {
            System.out.println("soft: " + buffer.size() + ">=" + size);
            LockSupport.parkNanos(PARK_NS);
            elapsedNs += PARK_NS;
        }
        System.out.println("wait: " + elapsedNs + ">=" + timeoutNs);
        if (elapsedNs >= timeoutNs)
            throw new RuntimeException("Not completed within timeout " + timeoutMs + " ms");
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private enum BatchAction {
        NONE,
        CHECK,
        DRAIN
    }

}
