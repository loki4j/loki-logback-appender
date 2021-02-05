package com.github.loki4j.logback;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import static org.junit.Assert.*;

import com.github.loki4j.common.LogRecord;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;
import com.google.protobuf.Timestamp;

import static com.github.loki4j.logback.Generators.*;

public class ProtobufEncoderTest {

    private LogRecord[] records(AbstractLoki4jEncoder e) {
        return new LogRecord[] {
            logRecord(100L, 1, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 1", e),
            logRecord(103L, 2, "level=DEBUG,app=my-app", "l=DEBUG c=test.TestApp t=thread-2 | Test message 2", e),
            logRecord(105L, 3, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-1 | Test message 3", e),
            logRecord(102L, 4, "level=INFO,app=my-app", "l=INFO c=test.TestApp t=thread-3 | Test message 4", e),
        };
    }

    private static ProtobufEncoder protobufEncoder(boolean staticLabels) {
        var encoder = new ProtobufEncoder();
        encoder.setStaticLabels(staticLabels);

        // we don't use these settings in tests
        // they are tested in AbstractLoki4jEncoderTest
        encoder.setLabel(labelCfg("level=%level,app=my-app", ",", "=", true));
        encoder.setMessage(messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"));
        encoder.setSortByTime(false);
        return encoder;
    }

    @Test
    public void testEncodeStaticLabels() {
        withEncoder(protobufEncoder(true), encoder -> {
            var expected = PushRequest.newBuilder()
                .addStreams(StreamAdapter.newBuilder()
                    .setLabels("{level=\"INFO\",app=\"my-app\"}")
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(100000001))
                        .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 1"))
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(103000002))
                        .setLine("l=DEBUG c=test.TestApp t=thread-2 | Test message 2"))
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(105000003))
                        .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 3"))
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(102000004))
                        .setLine("l=INFO c=test.TestApp t=thread-3 | Test message 4")))
                .build();
            try {
                var actual = PushRequest.parseFrom(Snappy.uncompress(encoder.encode(records(encoder))));
                assertEquals("static labels", expected, actual);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testEncodeDynamicLabels() {
        withEncoder(protobufEncoder(false), encoder -> {
            var expected = PushRequest.newBuilder()
                .addStreams(StreamAdapter.newBuilder()
                    .setLabels("{level=\"DEBUG\",app=\"my-app\"}")
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(103000002))
                        .setLine("l=DEBUG c=test.TestApp t=thread-2 | Test message 2")))
                .addStreams(StreamAdapter.newBuilder()
                    .setLabels("{level=\"INFO\",app=\"my-app\"}")
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(100000001))
                        .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 1"))
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(105000003))
                        .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 3"))
                    .addEntries(EntryAdapter.newBuilder()
                        .setTimestamp(Timestamp.newBuilder().setSeconds(0).setNanos(102000004))
                        .setLine("l=INFO c=test.TestApp t=thread-3 | Test message 4")))
                .build();
            try {
                var actual = PushRequest.parseFrom(Snappy.uncompress(encoder.encode(records(encoder))));
                assertEquals("dynamic labels", expected, actual);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
