package com.github.loki4j.client.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;

import com.github.loki4j.client.util.ByteBufferFactory;

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
        assertEquals(0, queue.getSizeBytes(), "no elements added yet");
        
        assertTrue(queue.offer(0, 1, 4, bb -> write(bb, new byte[] {0, 1, 2, 3})), "can add batch 0");
        assertEquals(4, queue.getSizeBytes(), "4 bytes added");

        assertTrue(queue.offer(1, 1, 4, bb -> write(bb, new byte[] {4, 5, 6, 7})), "can add batch 1");
        assertEquals(8, queue.getSizeBytes(), "8 bytes added");

        assertFalse(queue.offer(2, 1, 4, bb -> write(bb, new byte[] {8, 9, 10, 11})), "can not add batch 2");
        assertEquals(8, queue.getSizeBytes(), "still 8 bytes added");

        var binBatch0 = queue.borrowBuffer();
        assertEquals(0, binBatch0.batchId, "batch id");
        assertEquals(1, binBatch0.sizeItems, "batch items");
        assertEquals(4, binBatch0.sizeBytes, "batch bytes");
        assertArrayEquals(new byte[] {0, 1, 2, 3}, read(binBatch0), "batch data");
        queue.returnBuffer(binBatch0);

        assertTrue(queue.offer(2, 1, 4, bb -> write(bb, new byte[] {8, 9, 10, 11})), "can add batch 2");
        assertEquals(8, queue.getSizeBytes(), "again 8 bytes added");

        var binBatch1 = queue.borrowBuffer();
        assertEquals(1, binBatch1.batchId, "batch id");
        assertEquals(1, binBatch1.sizeItems, "batch items");
        assertEquals(4, binBatch1.sizeBytes, "batch bytes");
        assertArrayEquals(new byte[] {4, 5, 6, 7}, read(binBatch1), "batch data");
        queue.returnBuffer(binBatch1);

        var binBatch2 = queue.borrowBuffer();
        assertEquals(2, binBatch2.batchId, "batch id");
        assertEquals(1, binBatch2.sizeItems, "batch items");
        assertEquals(4, binBatch2.sizeBytes, "batch bytes");
        assertArrayEquals(new byte[] {8, 9, 10, 11}, read(binBatch2), "batch data");
        queue.returnBuffer(binBatch2);
    }

    @Test
    public void testBatchReuse() {
        var queue = new ByteBufferQueue(10, new ByteBufferFactory(false));
        assertEquals(null, queue.borrowBuffer(), "no buffer added yet");
        assertEquals(0, queue.poolSize(), "no batches in pool yet");

        assertTrue(queue.offer(0, 1, 4, bb -> write(bb, new byte[] {0, 1, 2, 3})), "can add batch 0");
        assertEquals(4, queue.getSizeBytes(), "4 bytes added");

        assertTrue(queue.offer(1, 1, 4, bb -> write(bb, new byte[] {4, 5, 6, 7})), "can add batch 1");
        assertEquals(8, queue.getSizeBytes(), "8 bytes added");
        assertEquals(0, queue.poolSize(), "no batches in pool yet");

        var binBatch0 = queue.borrowBuffer();
        queue.returnBuffer(binBatch0);
        assertEquals(1, queue.poolSize(), "1 batch in pool");
        var binBatch1 = queue.borrowBuffer();
        queue.returnBuffer(binBatch1);
        assertEquals(1, queue.poolSize(), "still 1 batch in pool");

        assertTrue(queue.offer(2, 1, 8, bb -> write(bb, new byte[] {0, 1, 2, 3, 4, 5, 6, 7})), "can add batch 2");
        assertEquals(8, queue.getSizeBytes(), "8 bytes added");
        assertEquals(0, queue.poolSize(), "batch from pool reused");
    }
    
}
