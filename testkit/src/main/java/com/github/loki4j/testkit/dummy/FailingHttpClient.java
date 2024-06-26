package com.github.loki4j.testkit.dummy;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.nio.ByteBuffer;

import com.github.loki4j.client.http.HttpStatus;
import com.github.loki4j.client.http.LokiResponse;

public class FailingHttpClient extends DummyHttpClient {

    private volatile FailureType failureType = FailureType.NONE;

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        var response = super.send(batch);
        if (failureType == FailureType.CONNECTION_EXCEPTION)
            throw new ConnectException("Text ConnectException");
        else if (failureType == FailureType.HTTP_CONNECT_TIMEOUT_EXCEPTION)
            throw new HttpConnectTimeoutException("Test HttpConnectTimeoutException");
        else if (failureType == FailureType.RATE_LIMITED)
            return new LokiResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate Limited Request");
        return response;
    }

    public void setFailure(FailureType failureType) {
        this.failureType = failureType;
    }

    public enum FailureType {
        NONE,
        CONNECTION_EXCEPTION,
        HTTP_CONNECT_TIMEOUT_EXCEPTION,
        RATE_LIMITED
    }

}
