package com.github.loki4j.common;

public interface EncoderFunction {

    int encode(LogRecord[] events, int eventsLen, byte[] output);

}
