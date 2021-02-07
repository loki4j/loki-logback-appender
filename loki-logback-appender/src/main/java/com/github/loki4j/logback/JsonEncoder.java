package com.github.loki4j.logback;

import com.github.loki4j.common.JsonWriter;
import com.github.loki4j.common.LogRecord;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into JSON format specified by Loki API
 */
@NoAutoStart
public class JsonEncoder extends AbstractLoki4jEncoder {

    private static final ThreadLocal<JsonWriter> localWriter =
        ThreadLocal.withInitial(() -> new JsonWriter());

    public String getContentType() {
        return "application/json";
    }

    @Override
    protected byte[] encodeStaticLabels(LogRecord[] batch) {
        var writer = localWriter.get();
        writer.beginStreams(batch[0], extractStreamKVPairs(batch[0].stream));
        for (int i = 1; i < batch.length; i++) {
            writer.nextRecord(batch[i]);
        }
        writer.endStreams();
        return writer.toByteArray();
    }

    @Override
    protected byte[] encodeDynamicLabels(LogRecord[] batch) {
        var writer = localWriter.get();
        var currentStream = batch[0].stream;
        writer.beginStreams(batch[0], extractStreamKVPairs(currentStream));
        for (int i = 1; i < batch.length; i++) {
            if (batch[i].stream != currentStream) {
                currentStream = batch[i].stream;
                writer.nextStream(batch[i], extractStreamKVPairs(currentStream));
            }
            else {
                writer.nextRecord(batch[i]);
            }
        }
        writer.endStreams();
        return writer.toByteArray();
    }

}
