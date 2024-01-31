package com.github.loki4j.logback;

import org.junit.Test;

import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.testkit.dummy.StringPayload;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectFormat() {
        withEncoder(toStringEncoder(
                labelCfg("level=%level,app=\"my\"app", "|", "~", true, false, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
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
                labelCfg(",,level=%level,,app=\"my\"app,", ",", "=", true, false, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs(",,level=INFO,,app=\"my\"app,test=test,");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });
    }

    @Test
    public void testExtractStreamKVPairsIgnoringBlank() {
        withEncoder(toStringEncoder(
                labelCfg(",,level=%level,,app=\"my\"app,", ",", "=", true, false, true),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,,app=\"my\"app,test=test,blank=");
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
                    true,
                    false,
                    false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
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
                labelCfg("level=%level,app=\"my\"app", ",", "=", true, false, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs1 = encoder.extractStreamKVPairs("level=INFO,app=\"my\"app,test=test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ,=", kvse1, kvs1);
        });

        withEncoder(toStringEncoder(
                labelCfg("level:%level;app:\"my\"app", ";", ":", true, false, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs2 = encoder.extractStreamKVPairs("level:INFO;app:\"my\"app;test:test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by ;:", kvse1, kvs2);
        });

        withEncoder(toStringEncoder(
                labelCfg("level.%level|app.\"my\"app", "|", ".", true, false, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false,
                false), encoder -> {
            var kvs3 = encoder.extractStreamKVPairs("level.INFO|app.\"my\"app|test.test");
            var kvse1 = new String[] {"level", "INFO", "app", "\"my\"app", "test", "test"};
            assertArrayEquals("Split by |.", kvse1, kvs3);
        });
    }

    @Test
    public void testMarker() {
        var staticMarker = LabelMarker.of("stcmrk", () -> "stat-val");
        var events = new ILoggingEvent[] {
            loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null, staticMarker),
            loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null, LabelMarker.of("mrk", () -> "mrk-val")),
            loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null, staticMarker),
            loggingEvent(104L, Level.INFO, "test.TestApp", "thread-1", "Test message 4", null, LabelMarker.of(() -> {
                var multipleLabels = new LinkedHashMap<String, String>();
                multipleLabels.put("mrk1", "v1");
                multipleLabels.put("mrk2", "v2");
                return multipleLabels;
            })),
        };

        var sender = dummySender();
        withAppender(
            appender(
                4,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, true, false), plainTextMsgLayout("%level | %msg"), false, false),
                sender), appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, no sort",
                StringPayload.builder()
                    .stream("[l, INFO, stcmrk, stat-val]",
                        "ts=100 INFO | Test message 1",
                        "ts=105 INFO | Test message 3")
                    .stream("[l, INFO, mrk, mrk-val]",
                        "ts=103 INFO | Test message 2")
                    .stream("[l, INFO, mrk1, v1, mrk2, v2]",
                        "ts=104 INFO | Test message 4")
                    .build(),
                StringPayload.parse(sender.lastBatch()));
            //System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testOrdering() {
        var eventsToOrder = new ILoggingEvent[] {
            loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null),
            loggingEvent(103L, Level.DEBUG, "test.TestApp", "thread-2", "Test message 2", null),
            loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null),
            loggingEvent(104L, Level.WARN, "test.TestApp", "thread-1", "Test message 4", null),
            loggingEvent(103L, Level.ERROR, "test.TestApp", "thread-2", "Test message 5", null),
            loggingEvent(110L, Level.INFO, "test.TestApp", "thread-2", "Test message 6", null),
        };

        var sender = dummySender();
        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false, false), plainTextMsgLayout("%level | %msg"), false, true),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "static labels, no sort",
                StringPayload.builder()
                    .stream("[l, INFO]",
                        "ts=105 INFO | Test message 1",
                        "ts=103 DEBUG | Test message 2",
                        "ts=100 INFO | Test message 3",
                        "ts=104 WARN | Test message 4",
                        "ts=103 ERROR | Test message 5",
                        "ts=110 INFO | Test message 6")
                    .build(),
                StringPayload.parse(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false, false), plainTextMsgLayout("%level | %msg"), true, true),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "static labels, sort by time",
                StringPayload.builder()
                    .stream("[l, INFO]",
                        "ts=100 INFO | Test message 3",
                        "ts=103 DEBUG | Test message 2",
                        "ts=103 ERROR | Test message 5",
                        "ts=104 WARN | Test message 4",
                        "ts=105 INFO | Test message 1",
                        "ts=110 INFO | Test message 6")
                    .build(),
                StringPayload.parse(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false, false), plainTextMsgLayout("%level | %msg"), false, false),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, no sort",
                StringPayload.builder()
                    .stream("[l, INFO]",
                        "ts=105 INFO | Test message 1",
                        "ts=100 INFO | Test message 3",
                        "ts=110 INFO | Test message 6")
                    .stream("[l, DEBUG]",
                        "ts=103 DEBUG | Test message 2")
                    .stream("[l, WARN]",
                        "ts=104 WARN | Test message 4")
                    .stream("[l, ERROR]",
                        "ts=103 ERROR | Test message 5")
                    .build(),
                StringPayload.parse(sender.lastBatch()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false, false), plainTextMsgLayout("%level | %msg"), true, false),
                sender), appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, sort by time",
                StringPayload.builder()
                    .stream("[l, INFO]",
                        "ts=100 INFO | Test message 3",
                        "ts=105 INFO | Test message 1",
                        "ts=110 INFO | Test message 6")
                    .stream("[l, DEBUG]",
                        "ts=103 DEBUG | Test message 2")
                    .stream("[l, WARN]",
                        "ts=104 WARN | Test message 4")
                    .stream("[l, ERROR]",
                        "ts=103 ERROR | Test message 5")
                    .build(),
                StringPayload.parse(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testNanoCounter() {
        var enc = toStringEncoder(
            labelCfg("app=my-app", ",", "=", true, false, false),
            plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg"),
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
