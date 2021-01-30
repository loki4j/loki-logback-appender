package com.github.loki4j.logback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
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
import com.github.loki4j.common.SoftLimitBuffer;

import ch.qos.logback.core.spi.LifeCycle;

public final class DefaultPipeline implements LifeCycle {

    private static final LogRecord[] ZERO_RECORDS = new LogRecord[0];

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(10);

    private final SoftLimitBuffer<LogRecord> buffer;

    private final AtomicReference<BatchAction> encodeAction = new AtomicReference<>(BatchAction.NONE);

    private final ArrayBlockingQueue<BinaryBatch> senderQueue = new ArrayBlockingQueue<>(10);

    private final Batcher batcher;

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

    public boolean append(Supplier<LogRecord> event) {
        if (buffer.offer(event)) {
            encodeAction.set(BatchAction.CHECK);
            return true;
        } else {
            return false;
        }
    }

    public void drain() {
        encodeAction.compareAndSet(BatchAction.NONE, BatchAction.DRAIN);
    }

    private void encoderProcess() {
        var pendingAction = encodeAction.get();
        var batch = new LogRecordBatch();
        while (started) {
            pendingAction = encodeAction.get();
            while (started && 
                    (pendingAction == BatchAction.NONE || senderQueue.remainingCapacity() == 0)) {
                LockSupport.parkNanos(this, PARK_NS);
                pendingAction = encodeAction.get();
            }
            if (!started) break;
            LogRecord[] records = ZERO_RECORDS;
            LogRecord record = buffer.poll();
            while(record != null && records.length == 0) {
                records = batcher.add(record, ZERO_RECORDS);
                if (records.length == 0) record = buffer.poll();
            }
            if (records.length == 0 && pendingAction == BatchAction.DRAIN)
                records = batcher.drain(lastSendTimeMs, ZERO_RECORDS);
            encodeAction.set(BatchAction.NONE);
            if (records.length == 0) continue;
            batch.init(records);
            var binBatch = encode.apply(batch);
            if (!senderQueue.offer(binBatch))
                throw new IllegalStateException("Sender queue was changed from another thread");
        }
    }

    private void senderProcess() throws InterruptedException {
        BinaryBatch batch = null;
        while(started && batch == null)
            batch = senderQueue.poll(PARK_NS, TimeUnit.NANOSECONDS);
        if (!started) return;

        final var batchToSend = batch;
        send
            .apply(batchToSend)
            .whenComplete((r, e) -> {
                buffer.commit(batchToSend.recordsCount);
                lastSendTimeMs = System.currentTimeMillis();
            });
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;        
    }

    private enum BatchAction {
        NONE,
        CHECK,
        DRAIN
    }


}
