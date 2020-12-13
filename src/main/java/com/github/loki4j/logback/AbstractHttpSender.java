package com.github.loki4j.logback;

import java.util.concurrent.CompletableFuture;

import com.github.loki4j.common.LokiResponse;

import ch.qos.logback.core.spi.ContextAwareBase;

public abstract class AbstractHttpSender extends ContextAwareBase {

    /**
     * Loki endpoint to be used for sending batches
     */
    protected String url;

    /**
     * Content-type header to send to Loki
     */
    protected String contentType;

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

    public void start() {}

    public void stop() {}

    public abstract CompletableFuture<LokiResponse> sendAsync(byte[] batch);

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    void setUrl(String url) {
        this.url = url;
    }

}
