package com.github.loki4j.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatcherTest {

    private final LogRecord[] EMPTY = new LogRecord[0];

    @Test 
    public void testAddInt() {
        var cbb = new Batcher(10, 0);
        var array1 = new LogRecord[10];
        var array2 = new LogRecord[10];

        for (int i = 0; i < 19; i++) {
            var record = LogRecord.create(i, 0, "stream", "message" + i);
            var buf = cbb.add(record, EMPTY);
            if (i < 10)
                array1[i] = record;
            else
                array2[i % 10] = record;

            if (i == 9) {
                assertEquals("Batch is ready", 10, buf.length);
                assertArrayEquals("Correct elements in batch", array1, buf);
            } else {
                assertEquals("Batch is not ready", 0, buf.length);
            }
        }

        var record = LogRecord.create(19, 0, "stream", "message" + 19);
        var buf = cbb.add(record, EMPTY);
        array2[9] = record;
        assertEquals("Batch is ready", 10, buf.length);
        assertArrayEquals("Correct elements in batch", array2, buf);
    }

    @Test
    public void testAddBatch1Int() {
        var cbb = new Batcher(1, 0);

        assertEquals("capacity is correct", 1, cbb.getCapacity());

        var record = LogRecord.create(1, 0, "stream", "message" + 1);;
        var buf = cbb.add(record, EMPTY);
        assertEquals("Batch is ready", 1, buf.length);
        assertArrayEquals("Correct elements in batch", new LogRecord[] { record }, buf);
    }

    @Test
    public void testDrain() {
        var cbb = new Batcher(10, 100);
        var array1 = new LogRecord[7];

        var buf = cbb.drain(System.currentTimeMillis() + 200, EMPTY);
        assertEquals("Batch is empty", 0, buf.length);

        for (int i = 0; i < 7; i++) {
            var record = LogRecord.create(i, 0, "stream", "message" + i);
            buf = cbb.add(record, EMPTY);
            array1[i] = record;
            assertEquals("Batch is not ready", 0, buf.length);
        }

        buf = cbb.drain(System.currentTimeMillis() - 200, EMPTY);
        assertEquals("Batch is ready", 7, buf.length);
        assertArrayEquals("Correct elements in batch", array1, buf);

        var record = LogRecord.create(1, 0, "stream", "message" + 1);;
        buf = cbb.add(record, EMPTY);
        assertEquals("Batch is not ready", 0, buf.length);
        buf = cbb.drain(System.currentTimeMillis() + 300, EMPTY);
        assertEquals("Batch is not ready", 0, buf.length);
    }

}
