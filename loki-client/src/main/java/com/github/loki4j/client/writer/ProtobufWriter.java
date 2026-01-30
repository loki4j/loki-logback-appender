package com.github.loki4j.client.writer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.pkg.google.protobuf.Timestamp;
import com.github.loki4j.pkg.loki.protobuf.Push.EntryAdapter;
import com.github.loki4j.pkg.loki.protobuf.Push.LabelPairAdapter;
import com.github.loki4j.pkg.loki.protobuf.Push.PushRequest;
import com.github.loki4j.pkg.loki.protobuf.Push.StreamAdapter;
import com.google.protobuf.CodedOutputStream;

import org.xerial.snappy.Snappy;

public final class ProtobufWriter implements Writer {

    private final ByteBuffer uncompressed;
    private final ByteBuffer compressed;

    private PushRequest.Builder request;
    private StreamAdapter.Builder stream;
    private int size = 0;

    public ProtobufWriter(int capacity, ByteBufferFactory bbFactory) {
        // allocating x1.5 of capacity, as compressed protobuf size
        // may be larger than uncompressed json
        var capacityX1_5 = capacity + capacity / 2;
        this.uncompressed = bbFactory.allocate(capacityX1_5);
        this.compressed = bbFactory.allocate(capacityX1_5);
        this.request = PushRequest.newBuilder();
    }

    public boolean isBinary() {
        return true;
    }

    public void serializeBatch(LogRecordBatch batch) {
        var currentStream = batch.get(0).stream;
        nextStream(currentStream);
        for (int i = 0; i < batch.size(); i++) {
            if (!batch.get(i).stream.equals(currentStream)) {
                currentStream = batch.get(i).stream;
                nextStream(currentStream);
            }
            nextEntry(batch.get(i));
        }
        try {
            endStreams();
        } catch (IOException e) {
            throw new RuntimeException("Protobuf encoding error", e);
        }
    }

    private void nextStream(Map<String, String> labelSet) {
        stream = request
            .addStreamsBuilder()
            .setLabels(label(labelSet));
    }

    static String label(Map<String, String> labels) {
        var s = new StringBuilder();
        s.append('{');
        var entries = labels.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            s.append(entry.getKey());
            s.append('=');
            s.append('"');
            s.append(entry.getValue().replace("\"", "\\\""));
            s.append('"');
            if (entries.hasNext())
                s.append(',');
        }
        s.append('}');
        return s.toString();
    }

    private void nextEntry(LogRecord record) {
        var entry = EntryAdapter.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(record.timestampMs / 1000)
                .setNanos((int)(record.timestampMs % 1000) * 1_000_000 + record.nanosInMs))
            .setLine(record.message);
        for (var kvp : record.metadata.entrySet()) {
            entry.addStructuredMetadata(LabelPairAdapter.newBuilder()
                .setName(kvp.getKey())
                .setValue(kvp.getValue())
            );
        }
        stream.addEntries(entry);
    }

    private void endStreams() throws IOException {
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
        buffer.flip();
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
