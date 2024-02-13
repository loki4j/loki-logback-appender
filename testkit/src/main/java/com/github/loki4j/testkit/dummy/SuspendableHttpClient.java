package com.github.loki4j.testkit.dummy;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import com.github.loki4j.client.http.LokiResponse;

public class SuspendableHttpClient extends DummyHttpClient {
    private final AtomicBoolean wait = new AtomicBoolean(false);

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        while(wait.get())
            LockSupport.parkNanos(1000);
        return super.send(batch);
    }

    public void suspend() {
        wait.set(true);
    }

    public void resume() {
        wait.set(false);
    }
}
