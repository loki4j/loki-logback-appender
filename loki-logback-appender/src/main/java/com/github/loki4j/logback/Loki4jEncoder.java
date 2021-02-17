package com.github.loki4j.logback;

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

}
