package com.github.loki4j.logback;

import com.github.loki4j.common.util.ByteBufferFactory;
import com.github.loki4j.common.writer.ProtobufWriter;
import com.github.loki4j.common.writer.Writer;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
@NoAutoStart
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    public Writer createWriter(int capacity, ByteBufferFactory bbFactory) {
        return new ProtobufWriter(capacity, bbFactory);
    }

    public String getContentType() {
        return "application/x-protobuf";
    }

}
