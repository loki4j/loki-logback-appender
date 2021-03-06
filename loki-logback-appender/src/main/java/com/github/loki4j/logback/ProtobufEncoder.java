package com.github.loki4j.logback;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.ProtobufWriter;
import com.github.loki4j.common.Writer;

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
