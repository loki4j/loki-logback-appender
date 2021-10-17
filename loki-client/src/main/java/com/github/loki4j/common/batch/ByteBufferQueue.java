package com.github.loki4j.common.batch;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.github.loki4j.common.util.ByteBufferFactory;

public class ByteBufferQueue {

    private final ConcurrentLinkedQueue<BinaryBatch> pool = new ConcurrentLinkedQueue<>();

    private final AtomicLong sizeBytes = new AtomicLong(0L);

    private final ConcurrentLinkedQueue<BinaryBatch> items = new ConcurrentLinkedQueue<>();

    private final long maxSizeBytes;
    private final ByteBufferFactory bufferFactory;

    public ByteBufferQueue(long maxSizeBytes, ByteBufferFactory bufferFactory) {
        this.maxSizeBytes = maxSizeBytes;
        this.bufferFactory = bufferFactory;
    }

    public boolean offer(long batchId, int itemsCount, int claimBytes, Consumer<ByteBuffer> write) {
        if (sizeBytes.get() + claimBytes > maxSizeBytes)
            return false;
        sizeBytes.addAndGet(claimBytes);

        var batch = pool.poll();
        if (batch == null)
            batch = new BinaryBatch();
        if (batch.data == null || batch.data.capacity() <= claimBytes)
            batch.data = bufferFactory.allocate(claimBytes + claimBytes / 2);
        batch.batchId = batchId;
        batch.sizeItems = itemsCount;
        batch.sizeBytes = claimBytes;
        batch.data.clear();
        write.accept(batch.data);
        items.offer(batch);

        return true;
    }

    public BinaryBatch borrowBuffer() {
        var batch = items.poll();
        if (batch != null)
            sizeBytes.addAndGet(-batch.sizeBytes);
        return batch;
    }

    public void returnBuffer(BinaryBatch batch) {
        if (!pool.isEmpty())
            return;
        pool.offer(batch);
    }

    public long getSizeBytes() {
        return sizeBytes.get();
    }

    int poolSize() {
        return pool.size();
    }
}
