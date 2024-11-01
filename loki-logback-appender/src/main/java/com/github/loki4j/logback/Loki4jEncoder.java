package com.github.loki4j.logback;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface for all Loki4j encoders.
 */
public interface Loki4jEncoder extends ContextAware, LifeCycle {

    LogRecordStream eventToStream(ILoggingEvent e);

    String eventToMessage(ILoggingEvent e);

    String[] eventToMetadata(ILoggingEvent e);

    WriterFactory getWriterFactory();

    boolean getStaticLabels();

}
