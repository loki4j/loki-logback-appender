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
    public void testDrain() {
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
}
