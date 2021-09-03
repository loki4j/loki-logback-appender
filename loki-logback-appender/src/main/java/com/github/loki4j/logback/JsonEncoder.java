package com.github.loki4j.logback;

import com.github.loki4j.common.pipeline.PipelineConfig;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Encoder that converts log batches into JSON format specified by Loki API
 */
@NoAutoStart
public class JsonEncoder extends AbstractLoki4jEncoder {

    @Override
    public PipelineConfig.WriterFactory getWriterFactory() {
        return PipelineConfig.json;
    }

}
