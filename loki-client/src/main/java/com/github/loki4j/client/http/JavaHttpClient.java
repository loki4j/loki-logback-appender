package com.github.loki4j.client.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import com.github.loki4j.client.util.Loki4jThreadFactory;

/**
 * Loki client that is backed by Java standard {@link java.net.http.HttpClient HttpClient}
 */
public final class JavaHttpClient implements Loki4jHttpClient {

    private final HttpConfig conf;
    private final HttpClient client;
    private final HttpRequest.Builder requestBuilder;

    private ExecutorService internalHttpThreadPool;

    public JavaHttpClient(HttpConfig conf) {
        this.conf = conf;

        internalHttpThreadPool = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            conf.java().innerThreadsExpirationMs, TimeUnit.MILLISECONDS, // expire unused threads after 5 batch intervals
            new SynchronousQueue<Runnable>(),
            new Loki4jThreadFactory("loki4j-java-http-internal"));

        client = HttpClient
            .newBuilder()
            .version(Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(conf.connectionTimeoutMs))
            .executor(internalHttpThreadPool)
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(conf.requestTimeoutMs))
            .uri(URI.create(conf.pushUrl))
            .header(HttpHeaders.CONTENT_TYPE, conf.contentType);

        conf.tenantId.ifPresent(tenant -> requestBuilder.setHeader(HttpHeaders.X_SCOPE_ORGID, tenant));
        conf.basicAuthToken().ifPresent(token -> requestBuilder.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + token));
    }

    @Override
    public void close() throws Exception {
        internalHttpThreadPool.shutdown();
    }

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        var request = requestBuilder
            .copy()
            .POST(HttpRequest.BodyPublishers.fromPublisher(new BatchPublisher(batch), batch.remaining()))
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return new LokiResponse(response.statusCode(), response.body());
    }

    @Override
    public HttpConfig getConfig() {
        return conf;
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
        private volatile boolean finished = false;

        public BatchSubscription(ByteBuffer body, Subscriber<? super ByteBuffer> subscriber) {
            this.body = body;
            this.subscriber = subscriber;
        }
        @Override
        public void request(long n) {
            if (cancelled || finished)
                return;  // no-op

            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("illegal non-positive request:" + n));
            } else {
                finished = true;
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
