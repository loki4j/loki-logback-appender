package com.github.loki4j.common.util;

/**
 * A lightweight framework-agnostic interface for internal logging
 */
public interface Loki4jLogger {

    void trace(String msg, Object... args);

    void info(String msg, Object... args);

    void warn(String msg, Object... args);

    void error(String msg, Object... args);

    void error(Throwable ex, String msg, Object... args);

}
