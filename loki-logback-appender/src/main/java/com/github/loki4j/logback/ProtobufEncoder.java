package com.github.loki4j.logback;

import java.io.IOException;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.ProtobufWriter;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
@NoAutoStart
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    private ProtobufWriter writer;

    public void initWriter(int capacity, ByteBufferFactory bbFactory) {
        writer = new ProtobufWriter(capacity, bbFactory);
    }

    public String getContentType() {
        return "application/x-protobuf";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecordBatch batch) {
        writer.nextStream(labels(extractStreamKVPairs(batch.get(0).stream)));
        for (int i = 0; i < batch.size(); i++) {
            writer.nextEntry(batch.get(i));
        }
        try {
            writer.endStreams();
            return writer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Protobuf encoding error", e);
        }
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecordBatch batch) {
        String currentStream = null;
        for (int i = 0; i < batch.size(); i++) {
            if (batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                writer.nextStream(labels(extractStreamKVPairs(currentStream)));
            }
            writer.nextEntry(batch.get(i));
        }
        try {
            writer.endStreams();
            return writer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Protobuf encoding error", e);
        }
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
    
}
