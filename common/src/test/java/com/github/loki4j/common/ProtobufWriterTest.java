package com.github.loki4j.common;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import static com.github.loki4j.common.LogRecord.create;
import static org.junit.Assert.*;

import java.io.IOException;

import com.google.protobuf.Timestamp;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;

public class ProtobufWriterTest {

    private LogRecord rec1 = create(1000, 0, "a=1,b=2", "TestMsg1");
    private LogRecord rec2 = create(2000, 0, "a=1,b=2", "TestMsg2");
    private LogRecord rec3 = create(3000, 0, "c=3,d=4", "TestMsg3");

    private PushRequest genPushRequest() {
        return PushRequest.newBuilder()
            .addStreams(StreamAdapter.newBuilder()
                .setLabels(rec1.stream)
                .addEntries(EntryAdapter.newBuilder()
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(rec1.timestampMs / 1000)
                        .setNanos(rec1.nanos))
                    .setLine(rec1.message))
                .addEntries(EntryAdapter.newBuilder()
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(rec2.timestampMs / 1000)
                        .setNanos(rec2.nanos))
                    .setLine(rec2.message)))
            .addStreams(StreamAdapter.newBuilder()
                .setLabels(rec3.stream)
                .addEntries(EntryAdapter.newBuilder()
                    .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(rec3.timestampMs / 1000)
                        .setNanos(rec3.nanos))
                    .setLine(rec3.message)))
            .build();
    }

    @Test
    public void testOnHeapWriter() throws IOException {
        var expUncomp = genPushRequest().toByteArray();
        var expComp = Snappy.compress(expUncomp);

        var writer = new ProtobufWriter(1000, new ByteBufferFactory(false));
        assertEquals("initial size is 0", 0, writer.size());
        writer.nextStream(rec1.stream);
        writer.nextEntry(rec1);
        writer.nextEntry(rec2);
        writer.nextStream(rec3.stream);
        writer.nextEntry(rec3);
        assertEquals("size before endStreams is 0", 0, writer.size());
        writer.endStreams();

        assertEquals("size is correct", expComp.length, writer.size());
        
        var actComp = writer.toByteArray();
        assertEquals("size reset", 0, writer.size());
        assertArrayEquals("compressed messages match", expComp, actComp);

        var actUncomp = Snappy.uncompress(actComp);
        assertArrayEquals("un-compressed messages match", expUncomp, actUncomp);
    }

    @Test
    public void testOffHeapWriter() throws IOException {
        var expUncomp = genPushRequest().toByteArray();
        var expComp = Snappy.compress(expUncomp);

        var writer = new ProtobufWriter(1000, new ByteBufferFactory(true));
        assertEquals("initial size is 0", 0, writer.size());
        writer.nextStream(rec1.stream);
        writer.nextEntry(rec1);
        writer.nextEntry(rec2);
        writer.nextStream(rec3.stream);
        writer.nextEntry(rec3);
        assertEquals("size before endStreams is 0", 0, writer.size());
        writer.endStreams();

        assertEquals("size is correct", expComp.length, writer.size());

        var actComp = writer.toByteArray();
        assertEquals("size reset", 0, writer.size());
        assertArrayEquals("compressed messages match", expComp, actComp);

        var actUncomp = Snappy.uncompress(actComp);
        assertArrayEquals("un-compressed messages match", expUncomp, actUncomp);
    }
    
}
