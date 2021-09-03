package com.github.loki4j.logback;

import com.github.loki4j.common.batch.LogRecordStream;
import com.github.loki4j.common.pipeline.PipelineConfig.WriterFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface for all Loki4j encoders
 */
public interface Loki4jEncoder extends ContextAware, LifeCycle {

    int timestampToNanos(long timestampMs);

    LogRecordStream eventToStream(ILoggingEvent e);

    String eventToMessage(ILoggingEvent e);

    WriterFactory getWriterFactory();

    boolean getSortByTime();

    boolean getStaticLabels();

}
