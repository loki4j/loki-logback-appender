package com.github.loki4j.logback;

import org.junit.Test;

import com.github.loki4j.client.util.OrderedMap;
import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;
import com.github.loki4j.testkit.dummy.StringPayload;
import com.github.loki4j.testkit.dummy.StringPayload.StringLogRecord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static com.github.loki4j.logback.Loki4jAppender.extractKVPairsFromPattern;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyPattern() {
        extractKVPairsFromPattern("", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsEmptyValue() {
        extractKVPairsFromPattern("level=,app=\"my\"app", ",", "=");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractStreamKVPairsIncorrectValue() {
        extractKVPairsFromPattern("level=%level app=\"my\"app", ",", "=");
    }

    @Test
    public void testExtractStreamKVPairsIgnoringEmpty() {
        var kvs1 = extractKVPairsFromPattern(",,level=%level,,app=\"my\"app,", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsIgnoringWhitespace() {
        var kvs1 = extractKVPairsFromPattern("\tlevel = %level,\n\tapp=\"my\"app,\n", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairsByRegex() {
        var kvs1 = extractKVPairsFromPattern(
                "\n\n// level is label\nlevel=%level\n// another comment\n\napp=\"my\"app\n\n// end comment",
                "regex:(\n|//[^\n]+)+",
                "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app");
        assertEquals("Split by ,=", kvse1, kvs1);
    }

    @Test
    public void testExtractStreamKVPairs() {
        var kvs1 = extractKVPairsFromPattern("level=%level,app=\"my\"app,test=test", ",", "=");
        var kvse1 = Map.of("level", "%level", "app", "\"my\"app", "test", "test");
        assertEquals("Split by ,=", kvse1, kvs1);

        var kvs2 = extractKVPairsFromPattern("level:%level;app:\"my\"app;test:test", ";", ":");
        assertEquals("Split by ;:", kvse1, kvs2);

        var kvs3 = extractKVPairsFromPattern("level.%level|app.\"my\"app|test.test", "|", ".");
        assertEquals("Split by |.", kvse1, kvs3);
    }

    @Test
    public void testLabelParsingFailed() {
        var event = loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null);

        var sender = dummySender();
        assertThrows("KV separation failed", IllegalArgumentException.class, () -> withAppender(stringAppender(
                "level=%level\napp=",
                null,
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                30,
                400L,
                sender), appender -> {
                    appender.append(event);
                    return null;
                }));
        assertThrows("Converter parsing failed", IllegalArgumentException.class, () -> withAppender(stringAppender(
                "level=%lev{,app=x",
                null,
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                30,
                400L,
                sender), appender -> {
                    appender.append(event);
                    return null;
                }));
    }

    @Test
    public void testLogRecordStreams() {
        withAppender(stringAppender(
                "level=%level\napp=my-app\nthread=%thread",
                null,
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                1,
                1000L,
                null), appender -> {
                    var stream1 = appender.eventToLogRecord(
                            loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null)).stream;
                    assertEquals(OrderedMap.of("level", "INFO", "app", "my-app", "thread", "thread-1"), stream1);

                    var stream2 = appender.eventToLogRecord(
                            loggingEvent(103L, Level.WARN, "test.TestApp", "thread-5", "Test message 2", null)).stream;
                    assertEquals(OrderedMap.of("level", "WARN", "app", "my-app", "thread", "thread-5"), stream2);

                    var stream3 = appender.eventToLogRecord(
                            loggingEvent(108L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null)).stream;
                    assertEquals(OrderedMap.of("level", "INFO", "app", "my-app", "thread", "thread-1"), stream3);

                    // The following no longer holds:
                    // assertTrue("Same labels resolved to one stream", stream1 == stream3);
                    // assertFalse("Different labels resolved to different streams", stream1 ==
                    // stream2);

                    return null;
                });
    }

    @Test
    public void testLabelValuesUnaffectedByKVSeparation() {
        withAppender(stringAppender(
                "level=%level\nclass=%logger\nthread=%thread",
                null,
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                1,
                1000L,
                null), appender -> {
                    var event = loggingEvent(100L, Level.INFO, "test.TestApp", "th=\n1", "Test message 1", null);
                    var record = appender.eventToLogRecord(event);
                    var stream1 = record.stream;
                    assertEquals(Map.of("level", "INFO", "class", "test.TestApp", "thread", "th=\n1"), stream1);

                    return null;
                });
    }

    @Test
    public void testLabelMarker() {
        var staticMarker = LabelMarker.of("stcmrk", () -> "stat-val");
        var events = new ILoggingEvent[] {
                loggingEvent(100L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null,
                        List.of(staticMarker)),
                loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null,
                        List.of(LabelMarker.of("mrk", () -> "mrk-val"))),
                loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 3", null,
                        List.of(staticMarker)),
                loggingEvent(104L, Level.INFO, "test.TestApp", "thread-1", "Test message 4", null,
                        List.of(LabelMarker.of(() -> {
                            var multipleLabels = new LinkedHashMap<String, String>();
                            multipleLabels.put("mrk1", "v1");
                            multipleLabels.put("mrk2", "v2");
                            return multipleLabels;
                        }))),
        };

        var sender = dummySender();
        var stringAppender = stringAppender(
                "l=%level",
                null,
                plainTextMsgLayout("%level | %msg"),
                4,
                1000L,
                sender);
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    "dynamic labels, no sort",
                    StringPayload.builder()
                            .stream(OrderedMap.of("l", "INFO", "stcmrk", "stat-val"),
                                    "ts=100 INFO | Test message 1",
                                    "ts=105 INFO | Test message 3")
                            .stream(OrderedMap.of("l", "INFO", "mrk", "mrk-val"),
                                    "ts=103 INFO | Test message 2")
                            .stream(OrderedMap.of("l", "INFO", "mrk1", "v1", "mrk2", "v2"),
                                    "ts=104 INFO | Test message 4")
                            .build(),
                    StringPayload.parse(sender.lastSendData()));
            // System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testMetadataMarker() {
        var events = new ILoggingEvent[] {
                loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null,
                        List.of(StructuredMetadataMarker.of("mrk", () -> "mrk-val"))),
                loggingEvent(104L, Level.INFO, "test.TestApp", "thread-1", "Test message 4", null,
                        List.of(StructuredMetadataMarker.of(() -> {
                            var multipleLabels = new LinkedHashMap<String, String>();
                            multipleLabels.put("mrk1", "v1");
                            multipleLabels.put("mrk2", "v2");
                            return multipleLabels;
                        }))),
        };

        var sender = dummySender();
        var stringAppender = stringAppender(
                "l=%level",
                "t=%thread\nc=%logger",
                plainTextMsgLayout("%level | %msg"),
                4,
                1000L,
                sender);
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    "dynamic labels, no sort",
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("l", "INFO"),
                                    StringLogRecord.of("ts=103 INFO | Test message 2",
                                            OrderedMap.of("t", "thread-2", "c", "test.TestApp", "mrk", "mrk-val")),
                                    StringLogRecord.of("ts=104 INFO | Test message 4",
                                            OrderedMap.of("t", "thread-1", "c", "test.TestApp", "mrk1", "v1", "mrk2",
                                                    "v2")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()));
            // System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testLabelAndMetadataMarker() {
        var events = new ILoggingEvent[] {
                loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null, List.of(
                        LabelMarker.of("label", () -> "label-val"),
                        StructuredMetadataMarker.of("meta", () -> "meta-val"))),
        };

        var sender = dummySender();
        var stringAppender = stringAppender(
                "l=%level",
                "t=%thread\nc=%logger",
                plainTextMsgLayout("%level | %msg"),
                4,
                1000L,
                sender);
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    "dynamic labels, no sort",
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("l", "INFO", "label", "label-val"),
                                    StringLogRecord.of("ts=103 INFO | Test message 2",
                                            OrderedMap.of("t", "thread-2", "c", "test.TestApp", "meta", "meta-val")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()));
            // System.out.println(new String(sender.lastBatch()));
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
        var staticAppender = stringAppender(
                "l=%level",
                null,
                plainTextMsgLayout("%level | %msg"),
                6,
                1000L,
                sender);
        staticAppender.getBatch().setStaticLabels(true);

        withAppender(staticAppender, appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                    "static labels, no sort",
                    StringPayload.builder()
                            .stream(OrderedMap.of("l", "INFO"),
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
                stringAppender(
                        "l=%level",
                        null,
                        plainTextMsgLayout("%level | %msg"),
                        6,
                        1000L,
                        sender),
                appender -> {
                    appender.append(eventsToOrder);
                    appender.waitAllAppended();
                    assertEquals(
                            "dynamic labels, no sort",
                            StringPayload.builder()
                                    .stream(OrderedMap.of("l", "INFO"),
                                            "ts=105 INFO | Test message 1",
                                            "ts=100 INFO | Test message 3",
                                            "ts=110 INFO | Test message 6")
                                    .stream(OrderedMap.of("l", "DEBUG"),
                                            "ts=103 DEBUG | Test message 2")
                                    .stream(OrderedMap.of("l", "WARN"),
                                            "ts=104 WARN | Test message 4")
                                    .stream(OrderedMap.of("l", "ERROR"),
                                            "ts=103 ERROR | Test message 5")
                                    .build(),
                            StringPayload.parse(sender.lastSendData()));
                    return null;
                });
    }
}
