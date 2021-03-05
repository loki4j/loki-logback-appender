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
    public byte[] encode(LogRecordBatch batch) {
        String currentStream = batch.get(0).stream;
        writer.nextStream(extractStreamKVPairs(currentStream));
        for (int i = 0; i < batch.size(); i++) {
            if (!staticLabels && batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                writer.nextStream(extractStreamKVPairs(currentStream));
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

}
