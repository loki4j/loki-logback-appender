package com.github.loki4j.common;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public final class BatchSendQueue {

    private final int BATCH_HEADER_LEN = 12; // 8:batchId + 4:len

    private final ByteBuffer buf;

    private final ReentrantLock lock;

    private volatile int batchesCount;

    private volatile int remainingBytes;

    public BatchSendQueue(ByteBuffer buf, int maxBatchBytes) {
        this.buf = buf;

        this.batchesCount = 0;
        recalcRemaining();

        this.lock = new ReentrantLock(true);
    }

    public boolean canWrite(int len) {
        return len <= remainingBytes;
    }

    public boolean canRead() {
        return batchesCount > 0;
    }

    public void write(long batchId, byte[] src) {
        if (remainingBytes < src.length)
            throw new IllegalStateException("No capacity to write");

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            buf.putLong(batchId);
            buf.putInt(src.length);
            buf.put(src);

            batchesCount++;
            recalcRemaining();
        } finally {
            lock.unlock();
        }
    }

    public void read(BinaryBatch binBatch) {
        if (batchesCount == 0)
            throw new IllegalStateException("No batches to read");

        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            buf.flip();
            var batchId = buf.getLong();
            var len = buf.getInt();
            binBatch.initFrom(batchId, len, dst -> buf.get(dst, 0, len));
            buf.compact();

            batchesCount--;
            recalcRemaining();
        } finally {
            lock.unlock();
        }
    }

    private void recalcRemaining() {
        remainingBytes = buf.remaining() - BATCH_HEADER_LEN;
    }
    
}
