package com.github.loki4j.logback;

import com.github.loki4j.common.LogRecord;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;

/**
 * Basic interface for all Loki4j layouts
 */
public interface Loki4jLayout extends Encoder<LogRecord[]> {
    
    public LogRecord eventToRecord(ILoggingEvent e, LogRecord r);

    public String getContentType();

}
