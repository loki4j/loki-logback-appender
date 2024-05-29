package com.github.loki4j.logback.access;

import com.github.loki4j.client.pipeline.PipelineConfig;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into Protobuf format specified by Loki API
 */
@NoAutoStart
public class ProtobufEncoder extends AbstractLoki4jEncoder {

    @Override
    public PipelineConfig.WriterFactory getWriterFactory() {
        return PipelineConfig.protobuf;
    }

}
