package com.github.loki4j.logback;

import java.io.IOException;

import com.github.loki4j.common.LogRecord;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.google.protobuf.Timestamp;

import org.xerial.snappy.Snappy;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
@NoAutoStart
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    public String getContentType() {
        return "application/x-protobuf";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecord[] batch) {
        var request = PushRequest.newBuilder();
        var streamBuilder = request
            .addStreamsBuilder()
            .setLabels(labels(extractStreamKVPairs(batch[0].stream)));
        for (int i = 0; i < batch.length; i++) {
            streamBuilder.addEntries(entry(batch[i]));
        }
        return compress(request.build().toByteArray());
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecord[] batch) {
        var request = PushRequest.newBuilder();
        var currentStream = batch[0].stream;
        var streamBuilder = request
            .addStreamsBuilder()
            .setLabels(labels(extractStreamKVPairs(currentStream)));
        for (int i = 0; i < batch.length; i++) {
            if (batch[i].stream != currentStream) {
                currentStream = batch[i].stream;
                streamBuilder = request
                    .addStreamsBuilder()
                    .setLabels(labels(extractStreamKVPairs(currentStream)));
            }
            streamBuilder.addEntries(entry(batch[i]));
        }
        return compress(request.build().toByteArray());
    }

    private String labels(String[] labels) {
        var s = new StringBuilder();
        s.append('{');
        if (labels.length > 0) {
            for (int i = 0; i < labels.length; i+=2) {
                s.append(labels[i]);
                s.append('=');
                s.append('"');
                s.append(labels[i + 1].replace("\"", "\\\""));
                s.append('"');
                if (i < labels.length - 2)
                    s.append(',');
            }
        }
        s.append('}');
        return s.toString();
    }
    
    private EntryAdapter entry(LogRecord record) {
        return EntryAdapter.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(record.timestampMs / 1000)
                .setNanos((int)(record.timestampMs % 1000) * 1_000_000 + record.nanos))
            .setLine(record.message)
            .build();
    }

    private byte[] compress(byte[] input) {
        try {
            return Snappy.compress(input);
        } catch (IOException e) {
            throw new RuntimeException("Snappy compression error", e);
        }
    }

}
