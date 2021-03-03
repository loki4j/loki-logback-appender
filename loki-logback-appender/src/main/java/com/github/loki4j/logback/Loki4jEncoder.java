package com.github.loki4j.logback;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;
import com.github.loki4j.common.Writer;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface for all Loki4j encoders
 */
public interface Loki4jEncoder extends ContextAware, LifeCycle {

    LogRecord eventToRecord(ILoggingEvent e);

    String getContentType();

    void setCapacity(int capacity);

    void setBufferFactory(ByteBufferFactory bufferFactory);

    Writer encode(LogRecordBatch batch);
}
