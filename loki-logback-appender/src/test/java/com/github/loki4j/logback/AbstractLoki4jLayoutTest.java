package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jLayoutTest {

    @Test
    public void testEventToRecord() {
        withLayout(defaultToStringLayout(), layout -> {
            var r1 = layout.eventToRecord(
                loggingEvent(
                    100L,
                    Level.INFO,
                    "test.TestApp",
                    "thread-1",
                    "Test message",
                    null),
                new LogRecord());
            var re1 = LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message ");
            assertEquals("Simple event", re1, r1);

            var r2 = layout.eventToRecord(
                loggingEvent(
                    102L,
                    Level.DEBUG,
                    "com.example.testtesttest.somepackage.TestApp",
                    "thread-2",
                    "Message with error",
                    exception("Test exception")),
                new LogRecord());
            // make the message a bit easier to check
            r2.message = r2.message.replaceAll("\r\n", "\n").replaceAll(":\\d+", "");

            var re2 = LogRecord.create(102L, 2, "level=DEBUG,app=my-app",
                    "l=DEBUG c=c.e.t.s.TestApp t=thread-2 | Message with error java.lang.RuntimeException: Test exception\n" +
                    "	at com.github.loki4j.logback.Generators.exception(Generators.java)\n");
            assertEquals("A bit more complex event", re2, r2);
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectFormat() {
        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", "|", "~", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), layout -> {
            var kvs1 = layout.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |~", kvse1, kvs1);
        });
    }

    @Test
    public void testExtractStreamKVPairs() {
        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), layout -> {
            var kvs1 = layout.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });

        withLayout(toStringLayout(
                labelCfg("level:%level;app:\"my\"app", ";", ":", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), layout -> {
            var kvs2 = layout.extractStreamKVPairs("level:INFO;app:\"my\"app;test:test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ;:", kvse1, kvs2);
        });

        withLayout(toStringLayout(
                labelCfg("level.%level|app.\"my\"app", "|", ".", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), layout -> {
            var kvs3 = layout.extractStreamKVPairs("level.INFO|app.\"my\"app|test.test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |.", kvse1, kvs3);
        });
    }


    @Test
    public void testEncode() {
        var rs = new LogRecord[] {
            LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
            LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
            LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
            LogRecord.create(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
            LogRecord.create(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
            LogRecord.create(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6")
        };

        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                true), layout -> {
            assertArrayEquals(new byte[0], layout.encode(new LogRecord[0]));
            assertEquals("static labels, no sort", batchToString(rs), new String(layout.encode(rs), layout.charset));
        });

        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                true,
                true), layout -> {
            assertEquals("static labels, sort by time", batchToString(new LogRecord[] {
                    LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    LogRecord.create(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    LogRecord.create(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                    LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    LogRecord.create(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6")
                }), new String(layout.encode(rs), layout.charset));
        });

        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), layout -> {
            assertEquals("dynamic labels, no sort", batchToString(new LogRecord[] {
                    LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    LogRecord.create(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    LogRecord.create(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                    LogRecord.create(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
                }), new String(layout.encode(rs), layout.charset));
        });

        withLayout(toStringLayout(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                true,
                false), layout -> {
            assertEquals("dynamic labels, sort by time", batchToString(new LogRecord[] {
                    LogRecord.create(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    LogRecord.create(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    LogRecord.create(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    LogRecord.create(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    LogRecord.create(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                    LogRecord.create(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
                }), new String(layout.encode(rs), layout.charset));
        });
    }
}
