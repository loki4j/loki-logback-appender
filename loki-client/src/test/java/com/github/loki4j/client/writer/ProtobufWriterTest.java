package com.github.loki4j.client.writer;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import static org.junit.Assert.*;

import java.io.IOException;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.pkg.google.protobuf.Timestamp;
import com.github.loki4j.pkg.loki.protobuf.Push.EntryAdapter;
import com.github.loki4j.pkg.loki.protobuf.Push.LabelPairAdapter;
import com.github.loki4j.pkg.loki.protobuf.Push.PushRequest;
import com.github.loki4j.pkg.loki.protobuf.Push.StreamAdapter;

public class ProtobufWriterTest {

    private LogRecordStream stream1 = LogRecordStream.create("level", "INFO", "app", "my-app");
    private LogRecordStream stream2 = LogRecordStream.create("level", "DEBUG", "app", "my-app");
    private String[] emptyMetadata = new String[0];
    private LogRecordBatch batch = new LogRecordBatch(new LogRecord[] {
        LogRecord.create(3000, 1, stream2, "l=DEBUG c=test.TestApp t=thread-2 | Test message 2", emptyMetadata),
        LogRecord.create(1000, 2, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 1", emptyMetadata),
        LogRecord.create(2000, 3, stream1, "l=INFO c=test.TestApp t=thread-3 | Test message 4", emptyMetadata),
        LogRecord.create(5000, 4, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 3", emptyMetadata),
    });

    private PushRequest expectedPushRequest = PushRequest.newBuilder()
        .addStreams(StreamAdapter.newBuilder()
            .setLabels("{level=\"DEBUG\",app=\"my-app\"}")
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(3).setNanos(1))
                .setLine("l=DEBUG c=test.TestApp t=thread-2 | Test message 2")
            )
        )
        .addStreams(StreamAdapter.newBuilder()
            .setLabels("{level=\"INFO\",app=\"my-app\"}")
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(1).setNanos(2))
                .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 1")
            )
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(2).setNanos(3))
                .setLine("l=INFO c=test.TestApp t=thread-3 | Test message 4")
            )
            .addEntries(EntryAdapter.newBuilder()
                .setTimestamp(Timestamp.newBuilder().setSeconds(5).setNanos(4))
                .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 3")
            )
        )
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

    public void testStructuredMetadata() throws IOException {
        String[] metadata1 = new String[] { "cluster", "clusterA", "traceId", "A00001" };
        String[] metadata2 = new String[] { "cluster", "clusterB", "traceId", "B56762" };
        LogRecordBatch metaBatch = new LogRecordBatch(new LogRecord[] {
            LogRecord.create(2000, 3, stream1, "l=INFO c=test.TestApp t=thread-3 | Test message 4", metadata1),
            LogRecord.create(5000, 4, stream1, "l=INFO c=test.TestApp t=thread-1 | Test message 3", metadata2),
        });

        PushRequest expectedMetaPushRequest = PushRequest.newBuilder()
            .addStreams(StreamAdapter.newBuilder()
                .setLabels("{level=\"INFO\",app=\"my-app\"}")
                .addEntries(EntryAdapter.newBuilder()
                    .setTimestamp(Timestamp.newBuilder().setSeconds(2).setNanos(3))
                    .setLine("l=INFO c=test.TestApp t=thread-3 | Test message 4")
                    .addStructuredMetadata(LabelPairAdapter.newBuilder().setName("cluster").setValue("clusterA"))
                    .addStructuredMetadata(LabelPairAdapter.newBuilder().setName("traceId").setValue("A00001"))
                )
                .addEntries(EntryAdapter.newBuilder()
                    .setTimestamp(Timestamp.newBuilder().setSeconds(5).setNanos(4))
                    .setLine("l=INFO c=test.TestApp t=thread-1 | Test message 3")
                    .addStructuredMetadata(LabelPairAdapter.newBuilder().setName("cluster").setValue("clusterB"))
                    .addStructuredMetadata(LabelPairAdapter.newBuilder().setName("traceId").setValue("B56762"))
                )
            )
            .build();

        var expUncomp = expectedMetaPushRequest.toByteArray();
        var expComp = Snappy.compress(expUncomp);

        var writer = new ProtobufWriter(1000, new ByteBufferFactory(false));
        assertEquals("initial size is 0", 0, writer.size());
        writer.serializeBatch(metaBatch);
        assertEquals("size is correct", expComp.length, writer.size());

        var actComp = writer.toByteArray();
        assertEquals("size reset", 0, writer.size());
        assertArrayEquals("compressed messages match", expComp, actComp);

        var actUncomp = Snappy.uncompress(actComp);
        assertArrayEquals("un-compressed messages match", expUncomp, actUncomp);
        assertEquals("deserialized", expectedMetaPushRequest, PushRequest.parseFrom(actUncomp));
    }
    
}
