package com.github.loki4j.logback.performance.reg_v120;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.JsonWriter;
import com.github.loki4j.common.LogRecordBatch;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into JSON format specified by Loki API
 */
@NoAutoStart
public class JsonEncoderV120a extends AbstractLoki4jEncoderV110 {

    private JsonWriter writer;

    public void initWriter(int capacity, ByteBufferFactory bbFactory) {
        writer = new JsonWriter(capacity);
    }

    public String getContentType() {
        return "application/json";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecordBatch batch) {
        writer.beginStreams(batch.get(0), extractStreamKVPairs(batch.get(0).stream));
        for (int i = 1; i < batch.size(); i++) {
            writer.nextRecord(batch.get(i));
        }
        writer.endStreams();
        return writer.toByteArray();
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecordBatch batch) {
        var currentStream = batch.get(0).stream;
        writer.beginStreams(batch.get(0), extractStreamKVPairs(currentStream));
        for (int i = 1; i < batch.size(); i++) {
            if (batch.get(i).stream != currentStream) {
                currentStream = batch.get(i).stream;
                writer.nextStream(batch.get(i), extractStreamKVPairs(currentStream));
            }
            else {
                writer.nextRecord(batch.get(i));
            }
        }
        writer.endStreams();
        return writer.toByteArray();
    }

}
