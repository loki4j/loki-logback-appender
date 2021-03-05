package com.github.loki4j.logback;

import java.util.Comparator;
import java.util.Optional;

import com.github.loki4j.common.ByteBufferFactory;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.common.LogRecordBatch;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;

/**
 * Basic interface for all Loki4j encoders
 */
public interface Loki4jEncoder extends Encoder<LogRecordBatch> {

    LogRecord eventToRecord(ILoggingEvent e);

    String getContentType();

    Optional<Comparator<LogRecord>> getLogRecordComparator();

    void setCapacity(int capacity);

    void setBufferFactory(ByteBufferFactory bufferFactory);

}
