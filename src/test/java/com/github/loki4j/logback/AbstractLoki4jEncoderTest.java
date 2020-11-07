package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test
    public void testEventToRecord() {
        var encoder = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false,
            false);
        encoder.start();

        var r1 = encoder.eventToRecord(
            loggingEvent(
                100L,
                Level.INFO,
                "test.TestApp",
                "thread-1",
                "Test message",
                null),
            new LogRecord());
        var re1 = logRecord(100L, 1, "level=INFO,app=\"my\"app", "l=INFO c=test.TestApp t=thread-1 | Test message ");
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
        var re2 = logRecord(102L, 2, "level=DEBUG,app=\"my\"app",
                "l=DEBUG c=c.e.t.s.TestApp t=thread-2 | Message with error java.lang.RuntimeException: Test exception\n" +
                "	at com.github.loki4j.logback.Generators.exception(Generators.java:14)\n");
        assertEquals("A bit more complex event", re2, r2);

        encoder.stop();
    }


    @Test
    public void testExtractStreamKVPairs() {
        var encoder1 = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false,
            false);
        encoder1.start();
        var kvs1 = encoder1.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
        var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
        assertArrayEquals("Split by ,=", kvse1, kvs1);
        encoder1.stop();

        var encoder2 = toStringEncoder(
            labelCfg("level:%level;app:\"my\"app", ";", ":", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false,
            false);
        encoder2.start();
        var kvs2 = encoder2.extractStreamKVPairs("level:INFO;app:\"my\"app;test:test");
        assertArrayEquals("Split by ;:", kvse1, kvs2);
        encoder2.stop();
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

        var encoder1 = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false,
            true);
        encoder1.start();
        assertEquals("static labels, no sort", batchToString(rs), new String(encoder1.encode(rs), encoder1.charset));
        encoder1.stop();

        var encoder2 = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            true,
            true);
        encoder2.start();
        assertEquals("static labels, sort by time", batchToString(new LogRecord[] {
                logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4"),
                logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6")
            }), new String(encoder2.encode(rs), encoder2.charset));
        encoder2.stop();

        var encoder3 = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            false,
            false);
        encoder3.start();
        assertEquals("dynamic labels, no sort", batchToString(new LogRecord[] {
                logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),
                logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
            }), new String(encoder3.encode(rs), encoder3.charset));
        encoder3.stop();

        var encoder4 = toStringEncoder(
            labelCfg("level=%level,app=\"my\"app", ",", "=", true),
            messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
            true,
            false);
        encoder4.start();
        assertEquals("dynamic labels, sort by time", batchToString(new LogRecord[] {
                logRecord(103L, 2, "level=DEBUG,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 2"),    
                logRecord(103L, 1, "level=ERROR,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 5"),
                logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
                logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
                logRecord(110L, 6, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-2 | Test message 6"),
                logRecord(104L, 4, "level=WARN,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 4")
            }), new String(encoder4.encode(rs), encoder4.charset));
        encoder4.stop();
    }
}
