package com.github.loki4j.common;

import java.io.IOException;

import com.google.protobuf.Timestamp;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;

import org.xerial.snappy.Snappy;

public class ProtobufWriter {

    public static PushRequest.Builder request() {
        return PushRequest.newBuilder();
    }

    public static StreamAdapter.Builder stream(String labels, PushRequest.Builder request) {
        return request
            .addStreamsBuilder()
            .setLabels(labels);
    }

    public static EntryAdapter entry(LogRecord record) {
        return EntryAdapter.newBuilder()
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(record.timestampMs / 1000)
                .setNanos((int)(record.timestampMs % 1000) * 1_000_000 + record.nanos))
            .setLine(record.message)
            .build();
    }

    public static byte[] compress(byte[] input) {
        try {
            return Snappy.compress(input);
        } catch (IOException e) {
            throw new RuntimeException("Snappy compression error", e);
        }
    }
    
}
