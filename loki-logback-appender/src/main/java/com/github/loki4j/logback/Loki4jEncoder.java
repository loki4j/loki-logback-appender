package com.github.loki4j.logback;

import java.util.Map;

import com.github.loki4j.client.pipeline.PipelineConfig.WriterFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface for all Loki4j encoders.
 */
public interface Loki4jEncoder extends ContextAware, LifeCycle {

    Map<String, String> eventToStream(ILoggingEvent e);

    String eventToMessage(ILoggingEvent e);

    Map<String, String> eventToMetadata(ILoggingEvent e);

    WriterFactory getWriterFactory();

    boolean getStaticLabels();

}
