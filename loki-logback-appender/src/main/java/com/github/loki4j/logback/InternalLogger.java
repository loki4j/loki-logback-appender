package com.github.loki4j.logback;

import com.github.loki4j.common.util.Loki4jLogger;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.WarnStatus;

/**
 * A logback-specific implementation of internal logging mechanism
 */
public class InternalLogger implements Loki4jLogger {

    private final Object source;
    private final ContextAware logger;

    public InternalLogger(Object source, ContextAware logger) {
        this.source = source;
        this.logger = logger;
    }
    
    @Override
    public void trace(String msg, Object... args) {
        if (isTraceEnabled(source))
            logger.addStatus(new InfoStatus(String.format(msg, args), source));
    }
    @Override
    public void info(String msg, Object... args) {
        logger.addStatus(new InfoStatus(String.format(msg, args), source));
    }
    @Override
    public void warn(String msg, Object... args) {
        logger.addStatus(new WarnStatus(String.format(msg, args), source));
    }
    @Override
    public void error(String msg, Object... args) {
        logger.addStatus(new ErrorStatus(String.format(msg, args), source));
    }
    @Override
    public void error(Throwable ex, String msg, Object... args) {
        logger.addStatus(new ErrorStatus(String.format(msg, args), source, ex));
    }
}
    