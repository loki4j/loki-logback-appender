package com.github.loki4j.logback.extractor;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * An interface for components that extract key-value pairs used for
 * Loki labels and structured metadata.
 */
public interface Extractor {

    void extract(ILoggingEvent event, Map<String, String> result);
}
