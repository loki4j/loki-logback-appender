package com.github.loki4j.common.util;

import java.nio.ByteBuffer;

public class ByteBufferFactory {

    private boolean isDirect = true;

    public ByteBufferFactory(boolean isDirect) {
        this.isDirect = isDirect;
    }
    
    public ByteBuffer allocate(int capacity) {
        return isDirect
            ? ByteBuffer.allocateDirect(capacity)
            : ByteBuffer.allocate(capacity);
    }
}
