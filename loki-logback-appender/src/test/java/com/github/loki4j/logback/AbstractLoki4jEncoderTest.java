package com.github.loki4j.logback;

import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

import com.github.loki4j.client.util.OrderedMap;
import com.github.loki4j.slf4j.marker.LabelMarker;
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;
import com.github.loki4j.testkit.dummy.StringPayload;
import com.github.loki4j.testkit.dummy.StringPayload.StringLogRecord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.loki4j.logback.Generators.*;

public class AbstractLoki4jEncoderTest {

    @Test
    public void testLabelParsingFailed() {
        var event = loggingEvent(105L, Level.INFO, "test.TestApp", "thread-1", "Test message 1", null);

        var sender = dummySender();
        assertThrows(IllegalArgumentException.class, () -> withAppender(
            appender(
                "level=%level\napp=",
                null,
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                batch(30, 400L),
                http(sender)), appender -> {
                    appender.append(event);
                    return null;
            }), "KV separation failed");
        assertThrows(IllegalArgumentException.class, () -> withAppender(
            appender(
                "level=%lev{,app=x",
                null,
                plainTextMsgLayout("l=%level c=%logger{20} t=%thread | %msg %ex{1}"),
                batch(30, 400L),
                http(sender)), appender -> {
                    appender.append(event);
                    return null;
                }), "Converter parsing failed");
    }

    @Test
    public void testLogRecordStreams() {
        withAppender(appender(
                "level=%level\napp=my-app\nthread=%thread",
                null,
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                batch(1, 1000L),
                http(null)), appender -> {
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
        withAppender(appender(
                "level=%level\nclass=%logger\nthread=%thread",
                null,
                plainTextMsgLayout("l=%level | %msg %ex{1}"),
                batch(1, 1000L),
                http(null)), appender -> {
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
        var stringAppender = appender(
                "l=%level",
                Loki4jAppender.DISABLE_SMD_PATTERN,
                plainTextMsgLayout("%level | %msg"),
                batch(4, 1000L),
                http(sender));
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .stream(OrderedMap.of("l", "INFO", "stcmrk", "stat-val"),
                                    "ts=100 INFO | Test message 1",
                                    "ts=105 INFO | Test message 3")
                            .stream(OrderedMap.of("l", "INFO", "mrk", "mrk-val"),
                                    "ts=103 INFO | Test message 2")
                            .stream(OrderedMap.of("l", "INFO", "mrk1", "v1", "mrk2", "v2"),
                                    "ts=104 INFO | Test message 4")
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "dynamic labels, no sort");
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
        var stringAppender = appender(
                "l=%level",
                "t=%thread\nc=%logger",
                plainTextMsgLayout("%level | %msg"),
                batch(2, 1000L),
                http(sender));
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("l", "INFO"),
                                    StringLogRecord.of("ts=103 INFO | Test message 2",
                                            OrderedMap.of("t", "thread-2", "c", "test.TestApp", "mrk", "mrk-val")),
                                    StringLogRecord.of("ts=104 INFO | Test message 4",
                                            OrderedMap.of("t", "thread-1", "c", "test.TestApp", "mrk1", "v1", "mrk2",
                                                    "v2")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "dynamic labels, no sort"
                );
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
        var stringAppender = appender(
                "l=%level",
                "t=%thread\nc=%logger",
                plainTextMsgLayout("%level | %msg"),
                batch(1, 1000L),
                http(sender));
        stringAppender.setReadMarkers(true);

        withAppender(stringAppender, appender -> {
            appender.append(events);
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("l", "INFO", "label", "label-val"),
                                    StringLogRecord.of("ts=103 INFO | Test message 2",
                                            OrderedMap.of("t", "thread-2", "c", "test.TestApp", "meta", "meta-val")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "dynamic labels, no sort"
                );
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
        var staticAppender = appender(
                "l=%level",
                Loki4jAppender.DISABLE_SMD_PATTERN,
                plainTextMsgLayout("%level | %msg"),
                batch(6, 1000L),
                http(sender));
        staticAppender.getBatch().setStaticLabels(true);

        withAppender(staticAppender, appender -> {
            appender.append(eventsToOrder);
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .stream(OrderedMap.of("l", "INFO"),
                                    "ts=105 INFO | Test message 1",
                                    "ts=103 DEBUG | Test message 2",
                                    "ts=100 INFO | Test message 3",
                                    "ts=104 WARN | Test message 4",
                                    "ts=103 ERROR | Test message 5",
                                    "ts=110 INFO | Test message 6")
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "static labels, no sort"
                );
            return null;
        });

        withAppender(
                appender(
                        "l=%level",
                        Loki4jAppender.DISABLE_SMD_PATTERN,
                        plainTextMsgLayout("%level | %msg"),
                        batch(6, 1000L),
                        http(sender)),
                appender -> {
                    appender.append(eventsToOrder);
                    appender.waitAllAppended();
                    assertEquals(
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
                            StringPayload.parse(sender.lastSendData()),
                            "dynamic labels, no sort");
                    return null;
                });
    }

    @Test
    public void testBulkPatterns() {
        var event = loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("kvp1", "kvpValue1"),
                new KeyValuePair("kvp2", "kvpValue2")
        ));
        event.getMDCPropertyMap().put("mdc1", "mdcValue1");
        event.getMDCPropertyMap().put("mdc2", "mdcValue2");

        var sender = dummySender();
        withAppender(appender(
                "* = %%mdc\nc=%logger",
                "t=%thread\n*=%%kvp",
                plainTextMsgLayout("%msg"),
                batch(1, 1000L),
                http(sender)), appender -> {
            appender.append(new ILoggingEvent[] { event });
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("c", "test.TestApp", "mdc1", "mdcValue1", "mdc2", "mdcValue2"),
                                    StringLogRecord.of("ts=103 Test message 2",
                                            OrderedMap.of("t", "thread-2", "kvp1", "kvpValue1", "kvp2", "kvpValue2")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "mdc+kvp");
            // System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testBulkPatternsNoValues() {
        var event = loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null);

        var sender = dummySender();
        withAppender(appender(
                "* = %%mdc\nc=%logger",
                "t=%thread\n*=%%kvp",
                plainTextMsgLayout("%msg"),
                batch(1, 1000L),
                http(sender)), appender -> {
            appender.append(new ILoggingEvent[] { event });
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("c", "test.TestApp"),
                                    StringLogRecord.of("ts=103 Test message 2", OrderedMap.of("t", "thread-2")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "mdc+kvp"
                );
            // System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }

    @Test
    public void testBulkPatternsIncludeExclude() {
        var event = loggingEvent(103L, Level.INFO, "test.TestApp", "thread-2", "Test message 2", null);
        event.setKeyValuePairs(Arrays.asList(
                new KeyValuePair("kvp1", "kvpValue1"),
                new KeyValuePair("kvp2", "kvpValue2")
        ));
        event.getMDCPropertyMap().put("mdc1", "mdcValue1");
        event.getMDCPropertyMap().put("mdc2", "mdcValue2");

        var sender = dummySender();
        withAppender(appender(
                "kvp_*=%%kvp{kvp1}\nc=%logger",
                "t=%thread\nmdc_* != %%mdc { mdc2 }",
                plainTextMsgLayout("%msg"),
                batch(1, 1000L),
                http(sender)), appender -> {
            appender.append(new ILoggingEvent[] { event });
            appender.waitAllAppended();
            assertEquals(
                    StringPayload.builder()
                            .streamWithMeta(OrderedMap.of("c", "test.TestApp", "kvp_kvp1", "kvpValue1"),
                                    StringLogRecord.of("ts=103 Test message 2",
                                            OrderedMap.of("t", "thread-2", "mdc_mdc1", "mdcValue1")))
                            .build(),
                    StringPayload.parse(sender.lastSendData()),
                    "mdc+kvp");
            // System.out.println(new String(sender.lastBatch()));
            return null;
        });
    }
}
