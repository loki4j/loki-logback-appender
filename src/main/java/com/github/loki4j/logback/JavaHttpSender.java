package com.github.loki4j.logback;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.common.LokiResponse;

/**
 * Loki sender that is backed by Java standard {@link java.net.http.HttpClient HttpClient}
 */
public class JavaHttpSender extends AbstractHttpSender {

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ExecutorService internalHttpThreadPool;

    @Override
    public void start(String contentType) {
        super.start(contentType);

        internalHttpThreadPool = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            getBatchTimeoutMs() * 5, TimeUnit.MILLISECONDS, // expire unused threads after 5 batch intervals
            new SynchronousQueue<Runnable>(),
            new LokiThreadFactory("loki-java-http-internal"));

        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(connectionTimeoutMs))
            .executor(internalHttpThreadPool)
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(requestTimeoutMs))
            .uri(URI.create(url))
            .header("Content-Type", contentType);
    }

    @Override
    public void stop() {
        internalHttpThreadPool.shutdown();

        super.stop();
    }

    @Override
    public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        // Java HttpClient natively supports async API
        // But we have to use its sync API to preserve the ordering of batches
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    var request = requestBuilder
                        .copy()
                        .POST(HttpRequest.BodyPublishers.ofByteArray(batch))
                        .build();

                    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    return new LokiResponse(response.statusCode(), response.body());
                } catch (Exception e) {
                    throw new RuntimeException("Error while sending batch to Loki", e);
                }
            }, httpThreadPool);
    }

}
