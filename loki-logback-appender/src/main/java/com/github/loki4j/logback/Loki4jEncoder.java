package com.github.loki4j.logback;

import com.github.loki4j.common.EncoderFunction;
import com.github.loki4j.common.LogRecord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;

/**
 * Basic interface for all Loki4j encoders
 */
public interface Loki4jEncoder extends Encoder<LogRecord[]>, EncoderFunction {
    
    LogRecord eventToRecord(ILoggingEvent e, LogRecord r);

    LogRecord eventToRecord(ILoggingEvent e);

    String getContentType();

}
