package com.github.loki4j.logback;

import java.util.Comparator;
import java.util.Optional;

import com.github.loki4j.common.batch.LogRecord;
import com.github.loki4j.common.batch.LogRecordStream;
import com.github.loki4j.common.util.ByteBufferFactory;
import com.github.loki4j.common.writer.Writer;

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

    String getContentType();

    Optional<Comparator<LogRecord>> getLogRecordComparator();

    Writer createWriter(int capacity, ByteBufferFactory bufferFactory);

}
