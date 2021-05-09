package com.github.loki4j.common.http;

public final class LokiResponse {
    public int status;
    public String body;
    
    public LokiResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }
}
