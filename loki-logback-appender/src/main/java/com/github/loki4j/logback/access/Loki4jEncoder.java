package com.github.loki4j.logback.access;

import com.github.loki4j.client.batch.LogRecordStream;
import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface for all Loki4j encoders.
 */
public interface Loki4jEncoder extends ContextAware, LifeCycle {

    LogRecordStream eventToStream(IAccessEvent e);

    String eventToMessage(IAccessEvent e);

    WriterFactory getWriterFactory();

    boolean getSortByTime();

    boolean getStaticLabels();

}
