package com.github.loki4j.common;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;

public class BatchSendQueueTest {

    @Test 
    public void testSingleThread() {
        var bsq = new BatchSendQueue(ByteBuffer.allocate(46), 4);
        assertTrue("can write from start", bsq.canWrite(34));
        assertFalse("can not read from start", bsq.canRead());

        var batch1 = new byte[] {1, 2, 3};
        bsq.write(1, batch1);
        assertTrue("can write 19 bytes", bsq.canWrite(19));
        assertFalse("can not write 20 bytes", bsq.canWrite(20));
        assertTrue("can read", bsq.canRead());

        var batch2 = new byte[] {4, 5, 6};
        bsq.write(2, batch2);
        assertTrue("can write 4 bytes", bsq.canWrite(4));
        assertTrue("can read", bsq.canRead());

        var batch3 = new byte[] {7, 8, 9, 0};
        bsq.write(3, batch3);
        assertFalse("can not write when filled", bsq.canWrite(1));
        assertTrue("can read", bsq.canRead());

        var actBatch = new BinaryBatch();
        bsq.read(actBatch);
        assertEquals("batchId match", 1, actBatch.batchId());
        assertArrayEquals(batch1, actBatch.toArray());
        assertTrue("can write 3 bytes", bsq.canWrite(3));
        assertTrue("can read", bsq.canRead());

        bsq.write(4, batch1);
        assertFalse("can not write when filled", bsq.canWrite(1));
        assertTrue("can read", bsq.canRead());

        bsq.read(actBatch);
        assertEquals("batchId match", 2, actBatch.batchId());
        assertArrayEquals(batch2, actBatch.toArray());
        bsq.read(actBatch);
        assertEquals("batchId match", 3, actBatch.batchId());
        assertArrayEquals(batch3, actBatch.toArray());
        bsq.read(actBatch);
        assertEquals("batchId match", 4, actBatch.batchId());
        assertArrayEquals(batch1, actBatch.toArray());
        assertTrue("can write from start", bsq.canWrite(10));
        assertFalse("can not read from start", bsq.canRead());
    }

}
