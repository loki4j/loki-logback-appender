package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test
    public void testEventToRecord() {
        withEncoder(defaultToStringEncoder(), encoder -> {
            var r1 = encoder.eventToRecord(
                loggingEvent(
                    100L,
                    Level.INFO,
                    "test.TestApp",
                    "thread-1",
                    "Test message",
                    null),
                new LogRecord());
            var re1 = logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message ");
            assertEquals("Simple event", re1, r1);

            var r2 = encoder.eventToRecord(
                loggingEvent(
                    102L,
                    Level.DEBUG,
                    "com.example.testtesttest.somepackage.TestApp",
                    "thread-2",
                    "Message with error",
                    exception("Test exception")),
                new LogRecord());
            var re2 = logRecord(102L, 2, "level=DEBUG,app=my-app",
                    "l=DEBUG c=c.e.t.s.TestApp t=thread-2 | Message with error java.lang.RuntimeException: Test exception\n" +
                    "	at com.github.loki4j.logback.Generators.exception(Generators.java:20)\n");
            assertEquals("A bit more complex event", re2, r2);
        });
    }

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


    @Test
    public void testEncode() {
        var rs = new LogRecord[] {
            logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
            logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
            logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
            logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
            logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
            logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6")
        };

        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                true), encoder -> {
            assertArrayEquals(new byte[0], encoder.encode(new LogRecord[0]));
            assertEquals("static labels, no sort", batchToString(rs), new String(encoder.encode(rs), encoder.charset));
        });

        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                true,
                true), encoder -> {
            assertEquals("static labels, sort by time", batchToString(new LogRecord[] {
                    logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                    logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6")
                }), new String(encoder.encode(rs), encoder.charset));
        });

        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            assertEquals("dynamic labels, no sort", batchToString(new LogRecord[] {
                    logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                    logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
                }), new String(encoder.encode(rs), encoder.charset));
        });

        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                true,
                false), encoder -> {
            assertEquals("dynamic labels, sort by time", batchToString(new LogRecord[] {
                    logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                    logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                    logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                    logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                    logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                    logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
                }), new String(encoder.encode(rs), encoder.charset));
        });
    }
}
