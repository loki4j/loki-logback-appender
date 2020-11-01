package com.github.loki4j.logback;

import com.github.loki4j.common.JsonWriter;
import com.github.loki4j.common.LogRecord;

/**
 * Encoder that converts log batches into JSON format specified by Loki API
 */
public class JsonEncoder extends AbstractLoki4jEncoder {

    public String getContentType() {
        return "application/json";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecord[] batch) {
        var writer = new JsonWriter(label.pairSeparator, label.keyValueSeparator);
        writer.beginStreams(batch[0]);
        for (int i = 1; i < batch.length; i++) {
            writer.nextRecord(batch[i]);
        }
        writer.endStreams();
        return writer.toByteArray();
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecord[] batch) {
        var writer = new JsonWriter(label.pairSeparator, label.keyValueSeparator);
        var currentStream = batch[0].stream;
        writer.beginStreams(batch[0]);
        for (int i = 1; i < batch.length; i++) {
            if (batch[i].stream != currentStream) {
                writer.nextStream(batch[i]);
                currentStream = batch[i].stream;
            }
            else {
                writer.nextRecord(batch[i]);
            }
        }
        writer.endStreams();
        return writer.toByteArray();
    }

}
