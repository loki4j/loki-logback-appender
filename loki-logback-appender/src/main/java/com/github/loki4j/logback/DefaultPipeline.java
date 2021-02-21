package com.github.loki4j.logback;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

    private final long PARK_NS = TimeUnit.MILLISECONDS.toNanos(1);

    private final SoftLimitBuffer<LogRecord> buffer;

    private final ArrayBlockingQueue<BinaryBatch> senderQueue = new ArrayBlockingQueue<>(10);

    private final Batcher batcher;

    private ScheduledExecutorService scheduler;
    private ExecutorService encoderThreadPool;
    private ExecutorService senderThreadPool;

    private final Function<LogRecordBatch, BinaryBatch> encode;

    private final Function<BinaryBatch, LokiResponse> send;

    private final boolean drainOnStop;

    private volatile boolean started = false;

    private AtomicBoolean newEventsArrived = new AtomicBoolean(false);
    private AtomicBoolean drainRequested = new AtomicBoolean(false);

    private AtomicLong lastSendTimeMs = new AtomicLong(System.currentTimeMillis());

    private ScheduledFuture<?> drainScheduledFuture;

    private boolean traceEnabled = false;

    public DefaultPipeline(
            SoftLimitBuffer<LogRecord> buffer,
            Batcher batcher,
            Function<LogRecordBatch, BinaryBatch> encode,
            Function<BinaryBatch, LokiResponse> send,
            boolean drainOnStop) {
        this.buffer = buffer;
        this.batcher = batcher;
        this.encode = encode;
        this.send = send;
        this.drainOnStop = drainOnStop;
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

    public boolean append(Supplier<LogRecord> event) {
        var accepted = buffer.offer(event);
        if (accepted)
            newEventsArrived.set(true);
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
        while (started &&
                (noEncodeActions() || senderQueue.remainingCapacity() == 0)) {
            LockSupport.parkNanos(this, PARK_NS);
        }
        if (!started) return;
        trace("check encode actions");
        LogRecord record = buffer.poll();
        while(record != null && batch.isEmpty()) {
            batcher.add(record, batch);
            if (batch.isEmpty()) record = buffer.poll();
        }
        if (batch.isEmpty() && drainRequested.get())
            batcher.drain(lastSendTimeMs.get(), batch);
        newEventsArrived.set(false);
        drainRequested.set(false);
        if (batch.isEmpty()) return;

        var binBatch = encode.apply(batch);
        batch.clear();
        var sent = false;
        while(started && !sent)
            sent = senderQueue.offer(binBatch, PARK_NS, TimeUnit.NANOSECONDS);
    }

    private boolean noEncodeActions() {
        return !newEventsArrived.get() && !drainRequested.get();
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
        trace("sent items: %s", batch.recordsCount);
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
        trace("wait send queue: started=%s, buffer(%s)>=%s, %s ms %s elapsed",
                started, buffer.size(), size, timeoutMs, elapsedNs < timeoutNs ? "not" : "");
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
