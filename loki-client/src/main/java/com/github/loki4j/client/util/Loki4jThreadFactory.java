package com.github.loki4j.client.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Loki4jThreadFactory implements ThreadFactory {

    private String namePrefix;

    private AtomicInteger counter;

    public Loki4jThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
        this.counter = new AtomicInteger(0);
    }

    @Override
    public Thread newThread(Runnable r) {
        var t = new Thread(r, namePrefix + "-" + counter.getAndIncrement());
        t.setDaemon(true);
        return t;
    }
    
}
