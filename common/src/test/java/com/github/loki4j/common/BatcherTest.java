package com.github.loki4j.common;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatcherTest {

    private static LogRecord logRecord(long ts) {
        return LogRecord.create(ts, 0, "stream", ("message" + ts).getBytes());
    }

    private static LogRecord logRecord(long ts, String stream, String message) {
        return LogRecord.create(ts, 0, stream, message.getBytes());
    }

    @Test 
    public void testAdd() {
        var cbb = new Batcher(10, 1000, 0);
        var buf = new LogRecordBatch(10);
        var array1 = new LogRecord[10];
        var array2 = new LogRecord[10];

        for (int i = 0; i < 19; i++) {
            var record = logRecord(i);
            cbb.add(record, buf);
            if (i < 10)
                array1[i] = record;
            else
                array2[i % 10] = record;

            if (i == 9) {
                assertEquals("Batch is ready", 10, buf.size());
                assertArrayEquals("Correct elements in batch", array1, buf.toArray());
                buf.clear();
            } else {
                assertEquals("Batch is not ready", 0, buf.size());
            }
        }

        var record = logRecord(19);
        cbb.add(record, buf);
        array2[9] = record;
        assertEquals("Batch is ready", 10, buf.size());
        assertArrayEquals("Correct elements in batch", array2, buf.toArray());
    }

    @Test
    public void testAddBatch1() {
        var cbb = new Batcher(1, 1000, 0);
        var buf = new LogRecordBatch(1);

        assertEquals("capacity is correct", 1, cbb.getCapacity());

        var record = logRecord(1);
        cbb.add(record, buf);
        assertEquals("Batch is ready", 1, buf.size());
        assertArrayEquals("Correct elements in batch", new LogRecord[] { record }, buf.toArray());
    }

    @Test
    public void testMessageTooLarge() {
        var cbb = new Batcher(1, 10, 0);
        var buf = new LogRecordBatch(1);

        assertEquals("capacity is correct", 1, cbb.getCapacity());

        var record = logRecord(1, "012", "3456789");
        assertFalse("Size too large", cbb.checkSize(record, buf));
        assertEquals("Batch is not ready", 0, buf.size());
    }

    @Test
    public void testSizeBatching() {
        var cbb = new Batcher(10, 30, 0);
        var buf = new LogRecordBatch(10);

        assertEquals("capacity is correct", 10, cbb.getCapacity());

        var r1 = logRecord(1, "012", "3456789");
        assertTrue("Size is ok", cbb.checkSize(r1, buf));
        cbb.add(r1, buf);
        assertEquals("Batch is not ready", 0, buf.size());

        var r2 = logRecord(2, "012", "abcdefghkl");
        assertTrue("Size is ok", cbb.checkSize(r2, buf));
        cbb.add(r2, buf);
        assertEquals("Batch is not ready", 0, buf.size());

        var r3 = logRecord(3, "012", "qwertyuiop");
        assertTrue("Size is ok", cbb.checkSize(r3, buf));
        assertEquals("Batch is ready", 2, buf.size());
        buf.clear();
        cbb.add(r3, buf);
        assertEquals("Batch is not ready", 0, buf.size());
    }

    @Test
    public void testDrain() {
        var cbb = new Batcher(10, 1000, 100);
        var buf = new LogRecordBatch(10);
        var array1 = new LogRecord[7];

        cbb.drain(System.currentTimeMillis() + 200, buf);
        assertEquals("Batch is empty", 0, buf.size());

        for (int i = 0; i < 7; i++) {
            var record = logRecord(i);
            cbb.add(record, buf);
            array1[i] = record;
            assertEquals("Batch is not ready", 0, buf.size());
        }

        cbb.drain(System.currentTimeMillis() - 200, buf);
        assertEquals("Batch is ready", 7, buf.size());
        assertArrayEquals("Correct elements in batch", array1, buf.toArray());
        buf.clear();

        var record = logRecord(1);
        cbb.add(record, buf);
        assertEquals("Batch is not ready", 0, buf.size());
        cbb.drain(System.currentTimeMillis() + 300, buf);
        assertEquals("Batch is not ready", 0, buf.size());
    }

}
