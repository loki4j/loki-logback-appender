package com.github.loki4j.common;

public class LogRecord {

    public long timestampMs;

    public int nanos;

    public String stream;

    public int streamHashCode;

    public String message;

    public static LogRecord create() {
        return new LogRecord();
    }

    @Override
    public String toString() {
        return "LogRecord [ts=" + timestampMs +
            ", nanos=" + nanos +
            ", stream=" + stream +
            ", message=" + message + "]";
    }

    
    
}
