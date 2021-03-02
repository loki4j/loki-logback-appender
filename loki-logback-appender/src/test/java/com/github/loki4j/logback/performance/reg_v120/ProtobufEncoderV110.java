package com.github.loki4j.logback.performance.reg_v120;

import java.io.IOException;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.google.protobuf.Timestamp;
import com.grafana.loki.protobuf.Logproto.EntryAdapter;
import com.grafana.loki.protobuf.Logproto.PushRequest;
import com.grafana.loki.protobuf.Logproto.StreamAdapter;

import org.xerial.snappy.Snappy;

public class ProtobufEncoderV110 extends AbstractLoki4jEncoder {

    public String getContentType() {
        return "application/x-protobuf";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecordBatch batch) {
        var request = PushRequest.newBuilder();
        var streamBuilder = request
            .addStreamsBuilder()
            .setLabels(labels(extractStreamKVPairs(batch.get(0).stream)));
        for (int i = 0; i < batch.size(); i++) {
            streamBuilder.addEntries(entry(batch.get(i)));
        }
        return compress(request.build().toByteArray());
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecordBatch batch) {
        var request = PushRequest.newBuilder();
        String currentStream = null;
        StreamAdapter.Builder streamBuilder = null;
        for (int i = 0; i < batch.size(); i++) {
            if (batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                streamBuilder = request
                    .addStreamsBuilder()
                    .setLabels(labels(extractStreamKVPairs(currentStream)));
            }
            streamBuilder.addEntries(entry(batch.get(i)));
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

    @Override
    public void initWriter(int capacity, ByteBufferFactory bbFactory) { }
    
}
