package com.github.loki4j.logback;

import org.junit.Test;

import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;
import com.github.loki4j.testkit.dummy.StringPayload;
import com.github.loki4j.testkit.dummy.StringPayload.StringLogRecord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyPattern() {
        AbstractLoki4jEncoder.extractKVPairsFromPattern("", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyValue() {
        AbstractLoki4jEncoder.extractKVPairsFromPattern("level=,app=\"my\"app", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectValue() {
        AbstractLoki4jEncoder.extractKVPairsFromPattern("level=%level app=\"my\"app", ",", "=");
    }

    @Test
    public void testExtractStreamKVPairsIgnoringEmpty() {
        var kvs1 = AbstractLoki4jEncoder.extractKVPairsFromPattern(",,level=%level,,app=\"my\"app,", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsIgnoringWhitespace() {
        var kvs1 = AbstractLoki4jEncoder.extractKVPairsFromPattern("\tlevel = %level,\n\tapp=\"my\"app,\n", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        var kvs1 = AbstractLoki4jEncoder.extractKVPairsFromPattern(
            "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
            "regex:(\n|//[^\n]+)+",
            "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairs() {
        var kvs1 = AbstractLoki4jEncoder.extractKVPairsFromPattern("level=%level,app=\"my\"app,test=test", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app", "test", "test");
        assertEquals("Split by ,=", kvse1, kvs1);

        var kvs2 = AbstractLoki4jEncoder.extractKVPairsFromPattern("level:%level;app:\"my\"app;test:test", ";", ":");
        assertEquals("Split by ;:", kvse1, kvs2);

        var kvs3 = AbstractLoki4jEncoder.extractKVPairsFromPattern("level.%level|app.\"my\"app|test.test", "|", ".");
        assertEquals("Split by |.", kvse1, kvs3);
    }

    @Test
    public void testLabelParsingFailed() {
        var event = loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null);

        var encoder1 = toStringEncoder(
                labelCfg("level=%level,app=", ",", "=", true, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false);
        var sender = dummySender();
        assertThrows("KV separation failed", IllegalArgumentException.class, () ->
            withAppender(appender(30, 400L, encoder1, sender), appender -> {
                appender.append(event);
                return null;
            })
        );
        var encoder2 = toStringEncoder(
                labelCfg("level=%lev{,app=x", ",", "=", true, false),
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                false);
                assertThrows("Converter parsing failed", IllegalArgumentException.class, () ->
        withAppender(appender(30, 400L, encoder2, sender), appender -> {
                appender.append(event);
                return null;
            })
        );
    }

    @Test
    public void testLogRecordStreams() {
        var encoder = toStringEncoder(
                labelCfg("level=%level,app=my-app,thread=%thread", ",", "=", true, false),
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                false);
        encoder.setContext(new LoggerContext());
        encoder.start();

        var stream1 = encoder.eventToStream(loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null));
        assertArrayEquals(new String[] { "level", "INFO", "app", "my-app", "thread", "thread-1" }, stream1.labels);

        var stream2 = encoder.eventToStream(loggingEvent(103L, Level.WARN, "test.TestApp", "thread-5", "Test message 2", null));
        assertArrayEquals(new String[] { "level", "WARN", "app", "my-app", "thread", "thread-5" }, stream2.labels);

        var stream3 = encoder.eventToStream(loggingEvent(108L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null));
        assertArrayEquals(new String[] { "level", "INFO", "app", "my-app", "thread", "thread-1" }, stream3.labels);

        assertTrue("Same labels resolved to one stream", stream1 == stream3);
        assertFalse("Different labels resolved to different streams", stream1 == stream2);

        encoder.stop();
    }

    @Test
    public void testLabelValuesUnaffectedByKVSeparation() {
        var encoder = toStringEncoder(
                labelCfg("level=%level.class=%logger.thread=%thread", ".", "=", true, false),
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                false);
        encoder.setContext(new LoggerContext());
        encoder.start();

        var stream1 = encoder.eventToStream(loggingEvent(100L, Level.INFO, "test.TestApp", "th=1", "Test message 1", null));
        assertArrayEquals(new String[] { "level", "INFO", "class", "test.TestApp", "thread", "th=1" }, stream1.labels);

        encoder.stop();
    }

    @Test
    public void testLabelMarker() {
        var staticMarker = LabelMarker.of("stcmrk", () -> "stat-val");
        var events = new ILoggingEvent[] {
            loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null, List.of(staticMarker)),
            loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null, List.of(LabelMarker.of("mrk", () -> "mrk-val"))),
            loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null, List.of(staticMarker)),
            loggingEvent(104L, Level.INFO, "test.TestApp", "thread-1", "Test message 4", null, List.of(LabelMarker.of(() -> {
                var multipleLabels = new LinkedHashMap<String, String>();
                multipleLabels.put("mrk1", "v1");
                multipleLabels.put("mrk2", "v2");
                return multipleLabels;
            }))),
        };

        var sender = dummySender();
        withAppender(
            appender(
                4,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, true), plainTextMsgLayout("%level | %msg"), false),
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
                StringPayload.parse(sender.lastSendData()));
            //System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testMetadataMarker() {
        var events = new ILoggingEvent[] {
            loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null, List.of(StructuredMetadataMarker.of("mrk", () -> "mrk-val"))),
            loggingEvent(104L, Level.INFO, "test.TestApp", "thread-1", "Test message 4", null, List.of(StructuredMetadataMarker.of(() -> {
                var multipleLabels = new LinkedHashMap<String, String>();
                multipleLabels.put("mrk1", "v1");
                multipleLabels.put("mrk2", "v2");
                return multipleLabels;
            }))),
        };

        var sender = dummySender();
        withAppender(
            appender(
                4,
                1000L,
                toStringEncoder(labelMetadataCfg("l=%level", "t=%thread,c=%logger", true), plainTextMsgLayout("%level | %msg"), false),
                sender), appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, no sort",
                StringPayload.builder()
                    .streamWithMeta("[l, INFO]",
                        StringLogRecord.of("[t, thread-2, c, test.TestApp, mrk, mrk-val]", "ts=103 INFO | Test message 2"))
                    .streamWithMeta("[l, INFO]",
                        StringLogRecord.of("[t, thread-1, c, test.TestApp, mrk1, v1, mrk2, v2]", "ts=104 INFO | Test message 4"))
                    .build(),
                StringPayload.parse(sender.lastSendData()));
            //System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testLabelAndMetadataMarker() {
        var events = new ILoggingEvent[] {
            loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null, List.of(
                    LabelMarker.of("label", () -> "label-val"),
                    StructuredMetadataMarker.of("meta", () -> "meta-val")
                )
            ),
        };

        var sender = dummySender();
        withAppender(
            appender(
                4,
                1000L,
                toStringEncoder(labelMetadataCfg("l=%level", "t=%thread,c=%logger", true), plainTextMsgLayout("%level | %msg"), false),
                sender), appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                "dynamic labels, no sort",
                StringPayload.builder()
                    .streamWithMeta("[l, INFO, label, label-val]",
                        StringLogRecord.of("[t, thread-2, c, test.TestApp, meta, meta-val]", "ts=103 INFO | Test message 2"))
                    .build(),
                StringPayload.parse(sender.lastSendData()));
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
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false), plainTextMsgLayout("%level | %msg"), true),
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
                StringPayload.parse(sender.lastSendData()));
            return null;
        });

        withAppender(
            appender(
                6,
                1000L,
                toStringEncoder(labelCfg("l=%level", ",", "=", true, false), plainTextMsgLayout("%level | %msg"), false),
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
                StringPayload.parse(sender.lastSendData()));
            return null;
        });
    }
}
