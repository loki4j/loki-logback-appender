package com.github.loki4j.logback;

import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract class that implements a common logic shared between standard
 * HTTP sender implementations
 */
public abstract class AbstractHttpSender extends ContextAwareBase implements HttpSender {

    /**
    * Loki endpoint to be used for sending batches
    */
    protected String url = "http://localhost:3100/loki/api/v1/push";

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

    private boolean started = false;

    public void start() {
        this.started = true;
    }

    public void stop() {
        this.started = false;
    }

    public boolean isStarted() {
        return started;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
