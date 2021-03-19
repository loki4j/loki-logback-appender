package com.github.loki4j.logback;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.LogRecordStream;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectFormat() {
        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", "|", "~", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |~", kvse1, kvs1);
        });
    }

    @Test
    public void testExtractStreamKVPairs() {
        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });

        withEncoder(toStringEncoder(
                labelCfg("level:%level;app:\"my\"app", ";", ":", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs2 = encoder.extractStreamKVPairs("level:INFO;app:\"my\"app;test:test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ;:", kvse1, kvs2);
        });

        withEncoder(toStringEncoder(
                labelCfg("level.%level|app.\"my\"app", "|", ".", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs3 = encoder.extractStreamKVPairs("level.INFO|app.\"my\"app|test.test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |.", kvse1, kvs3);
        });
    }

    private static void assertBatchesEqual(String msg, LogRecordBatch exp, LogRecordBatch act) {
        assertEquals(msg + ": size", exp.size(), act.size());
        for (int i = 0; i < exp.size(); i++)
            assertEquals(msg + ": item-" + i, exp.get(i), act.get(i));
    }

    private final static LogRecordStream streamInfo = LogRecordStream.create(0, "level", "INFO", "app", "my-app");
    private final static LogRecordStream streamDebug = LogRecordStream.create(1, "level", "DEBUG", "app", "my-app");
    private final static LogRecordStream streamWarn = LogRecordStream.create(2, "level", "WARN", "app", "my-app");
    private final static LogRecordStream streamError = LogRecordStream.create(3, "level", "ERROR", "app", "my-app");

    @Test
    public void testComparator() {
        Supplier<LogRecordBatch> rs = () -> new LogRecordBatch(new LogRecord[] {
            LogRecord.create(100L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
            LogRecord.create(103L, 0, streamDebug, "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
            LogRecord.create(105L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
            LogRecord.create(104L, 0, streamWarn, "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
            LogRecord.create(103L, 0, streamError, "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
            LogRecord.create(110L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-2 | Test message 6")
        });

        withEncoder(toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            false,
            true), e -> {
                assertEquals("static labels, no sort", Optional.empty(), e.getLogRecordComparator());
        });

        withEncoder(toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            true,
            true), e -> {
                var ars = rs.get();
                ars.sort(e.getLogRecordComparator().get());
                assertBatchesEqual("static labels, sort by time",
                    new LogRecordBatch(new LogRecord[] {
                        LogRecord.create(100L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                        LogRecord.create(103L, 0, streamDebug, "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                        LogRecord.create(103L, 0, streamError, "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                        LogRecord.create(104L, 0, streamWarn, "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                        LogRecord.create(105L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                        LogRecord.create(110L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-2 | Test message 6")
                    }),
                    ars);
        });

        withEncoder(toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            false,
            false), e -> {
                var ars = rs.get();
                ars.sort(e.getLogRecordComparator().get());
                assertBatchesEqual("dynamic labels, no sort",
                    new LogRecordBatch(new LogRecord[] {
                        LogRecord.create(100L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                        LogRecord.create(105L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                        LogRecord.create(110L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                        LogRecord.create(103L, 0, streamDebug, "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                        LogRecord.create(104L, 0, streamWarn, "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                        LogRecord.create(103L, 0, streamError, "l=INFO c=test.TestApp t=thread-2 | Test message 5")
                    }),
                    ars);
        });

        withEncoder(toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            true,
            false), e -> {
                var ars = rs.get();
                ars.sort(e.getLogRecordComparator().get());
                assertBatchesEqual("dynamic labels, sort by time",
                    new LogRecordBatch(new LogRecord[] {
                        LogRecord.create(100L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                        LogRecord.create(105L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                        LogRecord.create(110L, 0, streamInfo, "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                        LogRecord.create(103L, 0, streamDebug, "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                        LogRecord.create(104L, 0, streamWarn, "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                        LogRecord.create(103L, 0, streamError, "l=INFO c=test.TestApp t=thread-2 | Test message 5")
                    }),
                    ars);
        });
    }


    @Test
    public void testNanoCounter() {
        var enc = toStringEncoder(
            labelCfg("app=my-app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg"),
            true,
            false);

        assertEquals(123000, enc.timestampToNanos(1123));
        assertEquals(123001, enc.timestampToNanos(1123));
        assertEquals(123002, enc.timestampToNanos(1123));
        assertEquals(123003, enc.timestampToNanos(1123));

        assertEquals(122999, enc.timestampToNanos(1122));

        assertEquals(124000, enc.timestampToNanos(1124));
        for (int i = 0; i < 997; i++)
            enc.timestampToNanos(1124);
        assertEquals(124998, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
        assertEquals(124999, enc.timestampToNanos(1124));
    }
}
