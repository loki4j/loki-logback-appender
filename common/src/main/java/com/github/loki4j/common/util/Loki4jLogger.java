package com.github.loki4j.common.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A lightweight framework-agnostic interface for internal logging
 */
public interface Loki4jLogger {

    public static final Set<String> traceEnabledClasses =
        Arrays.stream(
            System.getProperty("loki4j.trace", "").split(",")
        ).collect(Collectors.toSet());

    default boolean isTraceEnabled(Object source) {
        return
            traceEnabledClasses.contains(source.getClass().getSimpleName());
    }

    void trace(String msg, Object... args);

    void info(String msg, Object... args);

    void warn(String msg, Object... args);

    void error(String msg, Object... args);

    void error(Throwable ex, String msg, Object... args);

}
