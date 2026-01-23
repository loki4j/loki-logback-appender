package com.github.loki4j.client.batch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class BatcherTest {

    private static LogRecord logRecord(long ts) {
        return LogRecord.create(ts, 0, Map.of("testkey", "testval"), ("message" + ts), Map.of());
    }

    private static LogRecord logRecord(long ts, Map<String, String> stream, String message) {
        return LogRecord.create(ts, 0, stream, message, Map.of());
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
        assertEquals("Correct batch condition", BatchCondition.MAX_ITEMS, buf.getCondition());
        assertArrayEquals("Correct elements in batch", new LogRecord[] { record }, buf.toArray());
    }

    @Test
    public void testMessageTooLarge() {
        var cbb = new Batcher(1, 10, 0);
        var buf = new LogRecordBatch(1);

        assertEquals("capacity is correct", 1, cbb.getCapacity());

        var record = logRecord(1, Map.of("a", "b"), "3456789");
        assertFalse("Size too large", cbb.validateLogRecordSize(record));
        assertEquals("Batch is not ready", 0, buf.size());
    }

    @Test
    public void testSizeBatching() {
        var cbb = new Batcher(10, 100, 0);
        var buf = new LogRecordBatch(10);

        assertEquals("capacity is correct", 10, cbb.getCapacity());

        var stream = Map.of("a", "b");

        var r1 = logRecord(1, stream, "123");
        assertTrue("Size is ok", cbb.validateLogRecordSize(r1));
        cbb.checkSizeBeforeAdd(r1, buf);
        cbb.add(r1, buf);
        assertEquals("Batch is not ready", 0, buf.size());

        var r2 = logRecord(2, stream, "abcdefghkl");
        assertTrue("Size is ok", cbb.validateLogRecordSize(r2));
        cbb.checkSizeBeforeAdd(r2, buf);
        cbb.add(r2, buf);
        assertEquals("Batch is not ready", 0, buf.size());

        var r3 = logRecord(3, stream, "qwertyuiop");
        assertTrue("Size is ok", cbb.validateLogRecordSize(r3));
        cbb.checkSizeBeforeAdd(r3, buf);
        assertEquals("Batch is ready", 2, buf.size());
        assertEquals("Correct batch condition", BatchCondition.MAX_BYTES, buf.getCondition());
        assertArrayEquals("Correct elements in batch", new LogRecord[]{r1, r2}, buf.toArray());

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
        assertEquals("Correct batch condition", BatchCondition.DRAIN, buf.getCondition());
        assertArrayEquals("Correct elements in batch", array1, buf.toArray());
        buf.clear();

        var record = logRecord(1);
        cbb.add(record, buf);
        assertEquals("Batch is not ready", 0, buf.size());
        cbb.drain(System.currentTimeMillis() + 300, buf);
        assertEquals("Batch is not ready", 0, buf.size());
    }

}
