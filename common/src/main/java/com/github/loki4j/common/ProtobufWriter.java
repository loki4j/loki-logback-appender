package com.github.loki4j.common;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Timestamp;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;

import org.xerial.snappy.Snappy;

public final class ProtobufWriter implements Writer {

    private final ByteBuffer uncompressed;
    private final ByteBuffer compressed;

    private PushRequest.Builder request;
    private StreamAdapter.Builder stream;
    private int size = 0;

    public ProtobufWriter(int capacity, ByteBufferFactory bbFactory) {
        this.uncompressed = bbFactory.allocate(capacity);
        this.compressed = bbFactory.allocate(capacity);
        this.request = PushRequest.newBuilder();
    }

    public void nextStream(String labels) {
        stream = request
            .addStreamsBuilder()
            .setLabels(labels);
    }

    public void nextEntry(LogRecord record) {
        stream.addEntries(EntryAdapter.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(record.timestampMs / 1000)
                .setNanos((int)(record.timestampMs % 1000) * 1_000_000 + record.nanos))
            .setLine(record.message));
    }

    public void endStreams() throws IOException {
        var writer = CodedOutputStream.newInstance(uncompressed);
        request.build().writeTo(writer);
        writer.flush();
        uncompressed.flip();
        if (uncompressed.hasArray()) {
            size = Snappy.compress(
                uncompressed.array(),
                0,
                uncompressed.limit(),
                compressed.array(),
                0);
            compressed.limit(size());
        } else {
            size = Snappy.compress(uncompressed, compressed);
        }
    }

    public int size() {
        return size;
    }

    public void toByteBuffer(ByteBuffer buffer) {
        buffer.put(compressed);
        reset();
    }

    public final byte[] toByteArray() {
        var result = new byte[compressed.remaining()];
        compressed.get(result);
        reset();
        return result;
    }

    /**
     * Resets the writer
     */
    public final void reset() {
        this.request = PushRequest.newBuilder();
        stream = null;
        size = 0;
        uncompressed.clear();
        compressed.clear();
    }

}
