package com.github.loki4j.common.http;

public final class LokiResponse {
    public final int status;
    public final String body;
    
    public LokiResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
}
