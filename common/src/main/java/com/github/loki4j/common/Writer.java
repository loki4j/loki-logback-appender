package com.github.loki4j.common;

import java.nio.ByteBuffer;

public interface Writer {

    int size();

    void toByteBuffer(ByteBuffer b);
    
}
