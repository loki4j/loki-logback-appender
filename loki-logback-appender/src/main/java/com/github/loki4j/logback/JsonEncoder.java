package com.github.loki4j.logback;

import com.github.loki4j.common.util.ByteBufferFactory;
import com.github.loki4j.common.writer.JsonWriter;
import com.github.loki4j.common.writer.Writer;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into JSON format specified by Loki API
 */
@NoAutoStart
public class JsonEncoder extends AbstractLoki4jEncoder {

    public Writer createWriter(int capacity, ByteBufferFactory bbFactory) {
        return new JsonWriter(capacity);
    }

    public String getContentType() {
        return "application/json";
    }

}
