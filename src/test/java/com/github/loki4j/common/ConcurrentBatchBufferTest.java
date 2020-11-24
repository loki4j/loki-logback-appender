package com.github.loki4j.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConcurrentBatchBufferTest {

    @Test 
    public void testAddInt() {
        var cbb = new ConcurrentBatchBuffer<Integer, Integer>(10, () -> 0, (i, e) -> i);
        var emptyArray = new Integer[0];

        for (int i = 0; i < 19; i++) {
            var buf = cbb.add(i, emptyArray);
            if (i == 9) {
                assertEquals("Batch is ready", 10, buf.length);
                assertArrayEquals("Correct elements in batch", new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, buf);
            } else {
                assertEquals("Batch is not ready", 0, buf.length);
            }
        }

        var buf = cbb.add(19, emptyArray);
        assertEquals("Batch is ready", 10, buf.length);
        assertArrayEquals("Correct elements in batch", new Integer[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, buf);
    }

    @Test
    public void testAddBatch1Int() {
        var cbb = new ConcurrentBatchBuffer<Integer, Integer>(1, () -> 0, (i, e) -> i);
        var emptyArray = new Integer[0];

        var buf = cbb.add(1, emptyArray);
        assertEquals("Batch is ready", 1, buf.length);
        assertArrayEquals("Correct elements in batch", new Integer[] {1}, buf);
    }

    @Test
    public void testDrainForce() {
        var cbb = new ConcurrentBatchBuffer<Integer, Integer>(10, () -> 0, (i, e) -> i);
        var emptyArray = new Integer[0];

        for (int i = 0; i < 7; i++) {
            var buf = cbb.add(i, emptyArray);
            assertEquals("Batch is not ready", 0, buf.length);
        }

        var buf = cbb.drain(-1, emptyArray);
        assertEquals("Batch is ready", 7, buf.length);
        assertArrayEquals("Correct elements in batch", new Integer[] {0, 1, 2, 3, 4, 5, 6}, buf);

        buf = cbb.drain(-1, emptyArray);
        assertEquals("Batch is not ready", 0, buf.length);
    }

    @Test
    public void testDrain() {
        var cbb = new ConcurrentBatchBuffer<Integer, Integer>(10, () -> 0, (i, e) -> i);
        var emptyArray = new Integer[0];

        for (int i = 0; i < 7; i++) {
            var buf = cbb.add(i, emptyArray);
            assertEquals("Batch is not ready", 0, buf.length);
        }

        var buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is not ready", 0, buf.length);

        try { Thread.sleep(200L); } catch (InterruptedException e1) { }

        buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is not ready", 0, buf.length);

        try { Thread.sleep(350L); } catch (InterruptedException e1) { }

        buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is ready", 7, buf.length);
        assertArrayEquals("Correct elements in batch", new Integer[] {0, 1, 2, 3, 4, 5, 6}, buf);

        buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is not ready", 0, buf.length);

        for (int i = 10; i < 17; i++) {
            buf = cbb.add(i, emptyArray);
            assertEquals("Batch is not ready", 0, buf.length);
        }

        buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is not ready", 0, buf.length);

        try { Thread.sleep(550L); } catch (InterruptedException e1) { }

        buf = cbb.drain(500, emptyArray);
        assertEquals("Batch is ready", 7, buf.length);
        assertArrayEquals("Correct elements in batch", new Integer[] {10, 11, 12, 13, 14, 15, 16}, buf);
    }
}
