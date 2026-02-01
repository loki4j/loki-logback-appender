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
                assertEquals(10, buf.size(), "Batch is ready");
                assertArrayEquals(array1, buf.toArray(), "Correct elements in batch");
                buf.clear();
            } else {
                assertEquals(0, buf.size(), "Batch is not ready");
            }
        }

        var record = logRecord(19);
        cbb.add(record, buf);
        array2[9] = record;
        assertEquals(10, buf.size(), "Batch is ready");
        assertArrayEquals(array2, buf.toArray(), "Correct elements in batch");
    }

    @Test
    public void testAddBatch1() {
        var cbb = new Batcher(1, 1000, 0);
        var buf = new LogRecordBatch(1);

        assertEquals(1, cbb.getCapacity(), "capacity is correct");

        var record = logRecord(1);
        cbb.add(record, buf);
        assertEquals(1, buf.size(), "Batch is ready");
        assertEquals(BatchCondition.MAX_ITEMS, buf.getCondition(), "Correct batch condition");
        assertArrayEquals(new LogRecord[] { record }, buf.toArray(), "Correct elements in batch");
    }

    @Test
    public void testMessageTooLarge() {
        var cbb = new Batcher(1, 10, 0);
        var buf = new LogRecordBatch(1);

        assertEquals(1, cbb.getCapacity(), "capacity is correct");

        var record = logRecord(1, Map.of("a", "b"), "3456789");
        assertFalse(cbb.validateLogRecordSize(record), "Size too large");
        assertEquals(0, buf.size(), "Batch is not ready");
    }

    @Test
    public void testSizeBatching() {
        var cbb = new Batcher(10, 100, 0);
        var buf = new LogRecordBatch(10);

        assertEquals(10, cbb.getCapacity(), "capacity is correct");

        var stream = Map.of("a", "b");

        var r1 = logRecord(1, stream, "123");
        assertTrue(cbb.validateLogRecordSize(r1), "Size is ok");
        cbb.checkSizeBeforeAdd(r1, buf);
        cbb.add(r1, buf);
        assertEquals(0, buf.size(), "Batch is not ready");

        var r2 = logRecord(2, stream, "abcdefghkl");
        assertTrue(cbb.validateLogRecordSize(r2), "Size is ok");
        cbb.checkSizeBeforeAdd(r2, buf);
        cbb.add(r2, buf);
        assertEquals(0, buf.size(), "Batch is not ready");

        var r3 = logRecord(3, stream, "qwertyuiop");
        assertTrue(cbb.validateLogRecordSize(r3), "Size is ok");
        cbb.checkSizeBeforeAdd(r3, buf);
        assertEquals(2, buf.size(), "Batch is ready");
        assertEquals(BatchCondition.MAX_BYTES, buf.getCondition(), "Correct batch condition");
        assertArrayEquals(new LogRecord[]{r1, r2}, buf.toArray(), "Correct elements in batch");

        buf.clear();
        cbb.add(r3, buf);
        assertEquals(0, buf.size(), "Batch is not ready");
    }

    @Test
    public void testDrain() {
        var cbb = new Batcher(10, 1000, 100);
        var buf = new LogRecordBatch(10);
        var array1 = new LogRecord[7];

        cbb.drain(System.currentTimeMillis() + 200, buf);
        assertEquals(0, buf.size(), "Batch is empty");

        for (int i = 0; i < 7; i++) {
            var record = logRecord(i);
            cbb.add(record, buf);
            array1[i] = record;
            assertEquals(0, buf.size(), "Batch is not ready");
        }

        cbb.drain(System.currentTimeMillis() - 200, buf);
        assertEquals(7, buf.size(), "Batch is ready");
        assertEquals(BatchCondition.DRAIN, buf.getCondition(), "Correct batch condition");
        assertArrayEquals(array1, buf.toArray(), "Correct elements in batch");
        buf.clear();

        var record = logRecord(1);
        cbb.add(record, buf);
        assertEquals(0, buf.size(), "Batch is not ready");
        cbb.drain(System.currentTimeMillis() + 300, buf);
        assertEquals(0, buf.size(), "Batch is not ready");
    }

}
