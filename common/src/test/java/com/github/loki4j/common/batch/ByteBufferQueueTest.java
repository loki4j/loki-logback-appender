package com.github.loki4j.common.batch;

import org.junit.Test;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import com.github.loki4j.common.util.ByteBufferFactory;

public class ByteBufferQueueTest {

    private static byte[] read(BinaryBatch bb) {
        var r = new byte[bb.data.remaining()];
        bb.data.get(r);
        return r;
    }

    private static ByteBuffer write(ByteBuffer bb, byte[] bs) {
        bb.put(bs);
        bb.flip();
        return bb;
    }

    @Test
    public void testMaxSize() {
        var queue = new ByteBufferQueue(10, new ByteBufferFactory(false));
        assertEquals("no elements added yet", 0, queue.getSizeBytes());
        
        assertTrue("can add batch 0", queue.offer(0, 1, 4, bb -> write(bb, new byte[] {0, 1, 2, 3})));
        assertEquals("4 bytes added", 4, queue.getSizeBytes());

        assertTrue("can add batch 1", queue.offer(1, 1, 4, bb -> write(bb, new byte[] {4, 5, 6, 7})));
        assertEquals("8 bytes added", 8, queue.getSizeBytes());

        assertFalse("can not add batch 2", queue.offer(2, 1, 4, bb -> write(bb, new byte[] {8, 9, 10, 11})));
        assertEquals("still 8 bytes added", 8, queue.getSizeBytes());

        var binBatch0 = queue.borrowBuffer();
        assertEquals("batch id", 0, binBatch0.batchId);
        assertEquals("batch items", 1, binBatch0.sizeItems);
        assertEquals("batch bytes", 4, binBatch0.sizeBytes);
        assertArrayEquals("batch data", new byte[] {0, 1, 2, 3}, read(binBatch0));
        queue.returnBuffer(binBatch0);

        assertTrue("can add batch 2", queue.offer(2, 1, 4, bb -> write(bb, new byte[] {8, 9, 10, 11})));
        assertEquals("again 8 bytes added", 8, queue.getSizeBytes());

        var binBatch1 = queue.borrowBuffer();
        assertEquals("batch id", 1, binBatch1.batchId);
        assertEquals("batch items", 1, binBatch1.sizeItems);
        assertEquals("batch bytes", 4, binBatch1.sizeBytes);
        assertArrayEquals("batch data", new byte[] {4, 5, 6, 7}, read(binBatch1));
        queue.returnBuffer(binBatch1);

        var binBatch2 = queue.borrowBuffer();
        assertEquals("batch id", 2, binBatch2.batchId);
        assertEquals("batch items", 1, binBatch2.sizeItems);
        assertEquals("batch bytes", 4, binBatch2.sizeBytes);
        assertArrayEquals("batch data", new byte[] {8, 9, 10, 11}, read(binBatch2));
        queue.returnBuffer(binBatch2);
    }

    @Test
    public void testBatchReuse() {
        var queue = new ByteBufferQueue(10, new ByteBufferFactory(false));
        assertEquals("no buffer added yet", null, queue.borrowBuffer());
        assertEquals("no batches in pool yet", 0, queue.poolSize());

        assertTrue("can add batch 0", queue.offer(0, 1, 4, bb -> write(bb, new byte[] {0, 1, 2, 3})));
        assertEquals("4 bytes added", 4, queue.getSizeBytes());

        assertTrue("can add batch 1", queue.offer(1, 1, 4, bb -> write(bb, new byte[] {4, 5, 6, 7})));
        assertEquals("8 bytes added", 8, queue.getSizeBytes());
        assertEquals("no batches in pool yet", 0, queue.poolSize());

        var binBatch0 = queue.borrowBuffer();
        queue.returnBuffer(binBatch0);
        assertEquals("1 batch in pool", 1, queue.poolSize());
        var binBatch1 = queue.borrowBuffer();
        queue.returnBuffer(binBatch1);
        assertEquals("still 1 batch in pool", 1, queue.poolSize());

        assertTrue("can add batch 2", queue.offer(2, 1, 8, bb -> write(bb, new byte[] {0, 1, 2, 3, 4, 5, 6, 7})));
        assertEquals("8 bytes added", 8, queue.getSizeBytes());
        assertEquals("batch from pool reused", 0, queue.poolSize());
    }
    
}
