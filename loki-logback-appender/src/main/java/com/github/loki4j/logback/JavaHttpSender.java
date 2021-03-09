package com.github.loki4j.logback;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.loki4j.common.HttpHeaders;
import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Loki sender that is backed by Java standard {@link java.net.http.HttpClient HttpClient}
 */
@NoAutoStart
public class JavaHttpSender extends AbstractHttpSender {

    /**
     * Maximum time that excess idle threads will wait for new
     * tasks before terminating inner HTTP threads
     */
    private long innerThreadsExpirationMs = 5 * 60_000;

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ExecutorService internalHttpThreadPool;

    @Override
    public void start() {
        internalHttpThreadPool = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            innerThreadsExpirationMs, TimeUnit.MILLISECONDS, // expire unused threads after 5 batch intervals
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
            .header(HttpHeaders.CONTENT_TYPE, contentType);

        super.start();

        tenantId.ifPresent(tenant -> requestBuilder.setHeader(HttpHeaders.X_SCOPE_ORGID,tenant));
        basicAuthToken.ifPresent(token -> requestBuilder.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + token));
    }

    @Override
    public void stop() {
        super.stop();
        internalHttpThreadPool.shutdown();
    }

    @Override
    public LokiResponse send(ByteBuffer batch) {
        try {
            var request = requestBuilder
                .copy()
                .POST(HttpRequest.BodyPublishers.fromPublisher(new BatchPublisher(batch), batch.remaining()))
                .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new LokiResponse(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new RuntimeException("Error while sending batch to Loki", e);
        }
    }

    public void setInnerThreadsExpirationMs(long innerThreadsExpirationMs) {
        this.innerThreadsExpirationMs = innerThreadsExpirationMs;
    }

    static class BatchPublisher implements Publisher<ByteBuffer> {
        private final ByteBuffer body;

        public BatchPublisher(ByteBuffer body) {
            this.body = body;
        }
        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new BatchSubscription(body, subscriber));
        }
    }

    static class BatchSubscription implements Subscription {
        private final ByteBuffer body;
        private final Subscriber<? super ByteBuffer> subscriber;
        private volatile boolean cancelled = false;

        public BatchSubscription(ByteBuffer body, Subscriber<? super ByteBuffer> subscriber) {
            this.body = body;
            this.subscriber = subscriber;
        }
        @Override
        public void request(long n) {
            if (cancelled)
                return;  // no-op

            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("illegal non-positive request:" + n));
            } else {
                subscriber.onNext(body);
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
