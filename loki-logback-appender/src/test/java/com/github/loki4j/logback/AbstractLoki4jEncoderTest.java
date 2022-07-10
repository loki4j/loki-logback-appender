package com.github.loki4j.logback;

import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

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
    public void testExtractStreamKVPairsIgnoringEmpty() {
        withEncoder(toStringEncoder(
                labelCfg(",,level=%level,,app=\"my\"app,", ",", "=", true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs(",,level=INFO,,app=\"my\"app,test=test,");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        withEncoder(toStringEncoder(
                labelCfg(
                    "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
                    "regex:(\n|//[^\n]+)+",
                    "=",
                    true),
                messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("\n\n// level is label\nlevel=INFO\n// another comment\n\napp=\"my\"app\n\n// end comment");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
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
    public void testOrdering() {
        var eventsToOrder = new ILoggingEvent[] {
            loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null),
            loggingEvent(103L, Level.DEBUG, "test.TestApp", "thread-2", "Test message 2", null),
            loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null),
            loggingEvent(104L, Level.WARN, "test.TestApp", "thread-1", "Test message 4", null),
            loggingEvent(103L, Level.ERROR, "test.TestApp", "thread-2", "Test message 5", null),
            loggingEvent(110L, Level.INFO, "test.TestApp", "thread-2", "Test message 6", null),
        };

        var sender = dummySender();
        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true), messageCfg("%level | %msg"), false, true),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "static labels, no sort",
                "LogRecord [ts=100, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 1]\n" +
                "LogRecord [ts=103, stream=Stream [id=0, labels=[l, INFO]], message=DEBUG | Test message 2]\n" +
                "LogRecord [ts=105, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 3]\n" +
                "LogRecord [ts=104, stream=Stream [id=0, labels=[l, INFO]], message=WARN | Test message 4]\n" +
                "LogRecord [ts=103, stream=Stream [id=0, labels=[l, INFO]], message=ERROR | Test message 5]\n" +
                "LogRecord [ts=110, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 6]\n",
                new String(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true), messageCfg("%level | %msg"), true, true),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "static labels, sort by time",
                "LogRecord [ts=100, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 1]\n" +
                "LogRecord [ts=103, stream=Stream [id=0, labels=[l, INFO]], message=DEBUG | Test message 2]\n" +
                "LogRecord [ts=103, stream=Stream [id=0, labels=[l, INFO]], message=ERROR | Test message 5]\n" +
                "LogRecord [ts=104, stream=Stream [id=0, labels=[l, INFO]], message=WARN | Test message 4]\n" +
                "LogRecord [ts=105, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 3]\n" +
                "LogRecord [ts=110, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 6]\n",
                new String(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true), messageCfg("%level | %msg"), false, false),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, no sort",
                "LogRecord [ts=100, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 1]\n" +
                "LogRecord [ts=105, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 3]\n" +
                "LogRecord [ts=110, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 6]\n" +
                "LogRecord [ts=103, stream=Stream [id=1, labels=[l, DEBUG]], message=DEBUG | Test message 2]\n" +
                "LogRecord [ts=104, stream=Stream [id=2, labels=[l, WARN]], message=WARN | Test message 4]\n" +
                "LogRecord [ts=103, stream=Stream [id=3, labels=[l, ERROR]], message=ERROR | Test message 5]\n",
                new String(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true), messageCfg("%level | %msg"), true, false),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, sort by time",
                "LogRecord [ts=100, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 1]\n" +
                "LogRecord [ts=105, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 3]\n" +
                "LogRecord [ts=110, stream=Stream [id=0, labels=[l, INFO]], message=INFO | Test message 6]\n" +
                "LogRecord [ts=103, stream=Stream [id=1, labels=[l, DEBUG]], message=DEBUG | Test message 2]\n" +
                "LogRecord [ts=104, stream=Stream [id=2, labels=[l, WARN]], message=WARN | Test message 4]\n" +
                "LogRecord [ts=103, stream=Stream [id=3, labels=[l, ERROR]], message=ERROR | Test message 5]\n",
                new String(sender.lastBatch()));
            return null;
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
