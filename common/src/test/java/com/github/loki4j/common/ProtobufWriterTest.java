package com.github.loki4j.common;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import static org.junit.Assert.*;

import java.io.IOException;

import com.google.protobuf.Timestamp;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;

public class ProtobufWriterTest {

    private LogRecordStream stream1 = LogRecordStream.create(0, "level", "INFO", "app", "my-app");
    private LogRecordStream stream2 = LogRecordStream.create(1, "level", "DEBUG", "app", "my-app");
    private LogRecordBatch batch = new LogRecordBatch(new LogRecord[] {
        LogRecord.create(3000, stream2, "l=DEBUG c=test.TestApp t=thread-2 | Test message 2"),
        LogRecord.create(1000, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 1"),
        LogRecord.create(2000, stream1, "l=INFO c=test.TestApp t=thread-3 | Test message 4"),
        LogRecord.create(5000, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 3"),
    });

    private PushRequest expectedPushRequest = PushRequest.newBuilder()
        .addStreams(StreamAdapter.newBuilder()
            .setLabels("{level=\"DEBUG\",app=\"my-app\"}")
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(3).setNanos(0))
                .setLine("l=DEBUG c=test.TestApp t=thread-2 | Test message 2")))
        .addStreams(StreamAdapter.newBuilder()
            .setLabels("{level=\"INFO\",app=\"my-app\"}")
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(1).setNanos(0))
                .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 1"))
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(2).setNanos(0))
                .setLine("l=INFO c=test.TestApp t=thread-3 | Test message 4"))
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(5).setNanos(0))
                .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 3")))
        .build();

    @Test
    public void testOnHeapWriter() throws IOException {
        var expUncomp = expectedPushRequest.toByteArray();
        var expComp = Snappy.compress(expUncomp);

        var writer = new ProtobufWriter(1000, new ByteBufferFactory(false));
        assertEquals("initial size is 0", 0, writer.size());
        writer.serializeBatch(batch);
        assertEquals("size is correct", expComp.length, writer.size());
        
        var actComp = writer.toByteArray();
        assertEquals("size reset", 0, writer.size());
        assertArrayEquals("compressed messages match", expComp, actComp);

        var actUncomp = Snappy.uncompress(actComp);
        assertArrayEquals("un-compressed messages match", expUncomp, actUncomp);
        assertEquals("deserialized", expectedPushRequest, PushRequest.parseFrom(actUncomp));
    }

    @Test
    public void testOffHeapWriter() throws IOException {
        var expUncomp = expectedPushRequest.toByteArray();
        var expComp = Snappy.compress(expUncomp);

        var writer = new ProtobufWriter(1000, new ByteBufferFactory(true));
        assertEquals("initial size is 0", 0, writer.size());
        writer.serializeBatch(batch);
        assertEquals("size is correct", expComp.length, writer.size());

        var actComp = writer.toByteArray();
        assertEquals("size reset", 0, writer.size());
        assertArrayEquals("compressed messages match", expComp, actComp);

        var actUncomp = Snappy.uncompress(actComp);
        assertArrayEquals("un-compressed messages match", expUncomp, actUncomp);
        assertEquals("deserialized", expectedPushRequest, PushRequest.parseFrom(actUncomp));
    }
    
}
