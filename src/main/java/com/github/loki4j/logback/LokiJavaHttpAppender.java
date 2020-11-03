package com.github.loki4j.logback;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Loki appender that is backed by Java standard {@link java.net.http.HttpClient HttpClient}
 */
public class LokiJavaHttpAppender extends AbstractLoki4jAppender {

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    @Override
    protected void startHttp(String contentType) {
        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(connectionTimeoutMs))
            .executor(httpThreadPool)
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(requestTimeoutMs))
            .uri(URI.create(url))
            .header("Content-Type", contentType);
    }

    @Override
    protected void stopHttp() {
        httpThreadPool.shutdown();
    }

    @Override
    protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        var request = requestBuilder
            .copy()
            .POST(HttpRequest.BodyPublishers.ofByteArray(batch))
            .build();

        return client
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(r -> new LokiResponse(r.statusCode(), r.body()));
    }

}
