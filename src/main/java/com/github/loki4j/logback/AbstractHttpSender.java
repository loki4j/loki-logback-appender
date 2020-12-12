package com.github.loki4j.logback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.loki4j.common.LokiResponse;

public abstract class AbstractHttpSender {

    /**
     * Loki endpoint to be used for sending batches
     */
    protected String url = "http://localhost:3100/loki/api/v1/push";

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    protected long connectionTimeoutMs = 30_000;
    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    protected long requestTimeoutMs = 5_000;

    /**
     * Number of threads to use for sending HTTP requests
     */
    private int httpThreads = 1;

    protected ExecutorService httpThreadPool;

    
    public void start(String contentType) {
        httpThreadPool = Executors.newFixedThreadPool(httpThreads, new LokiThreadFactory("loki-http-sender"));
    }

    public void stop() {
        httpThreadPool.shutdown();
    }

    public abstract CompletableFuture<LokiResponse> sendAsync(byte[] batch);

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

}
