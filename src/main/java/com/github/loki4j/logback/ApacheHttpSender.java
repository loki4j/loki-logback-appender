package com.github.loki4j.logback;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.github.loki4j.common.LokiResponse;
import com.github.loki4j.common.LokiThreadFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Loki sender that is backed by Apache {@link org.apache.http.client.HttpClient HttpClient}
 */
public class ApacheHttpSender extends AbstractHttpSender {

    /**
     * Maximum number of HTTP connections setting for HttpClient
     */
    private int maxConnections = 1;

    /**
     * A duration of time which the connection can be safely kept
     * idle for later reuse. This value can not be  greater than
     * server.http-idle-timeout in your Loki config
     */
    private long connectionKeepAliveMs = 120_000;

    /**
     * Number of threads to use for sending HTTP requests
     */
    private int httpThreads = 1;

    private ExecutorService httpThreadPool;

    private CloseableHttpClient client;
    private Function<byte[], HttpPost> requestBuilder;

    @Override
    public void start() {
        httpThreadPool = Executors.newFixedThreadPool(httpThreads, new LokiThreadFactory("loki-http-sender"));

        var cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnections);
        cm.setDefaultMaxPerRoute(maxConnections);

        client = HttpClients
            .custom()
            .setConnectionManager(cm)
            .setKeepAliveStrategy(new ConnectionKeepAliveStrategy(){
                @Override
                public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                    return connectionKeepAliveMs;
                }
            })
            .setDefaultRequestConfig(RequestConfig
                .custom()
                .setSocketTimeout((int)connectionTimeoutMs)
                .setConnectTimeout((int)connectionTimeoutMs)
                .setConnectionRequestTimeout((int)requestTimeoutMs)
                .build())
            .build();
        
        requestBuilder = (body) -> {
            var request = new HttpPost(url);
            request.addHeader("Content-Type", contentType);
            request.setEntity(new ByteArrayEntity(body));
            return request;
        };

        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        try {
            client.close();
        } catch (IOException e) {
            addWarn("Error while closing Apache HttpClient", e);
        }
        httpThreadPool.shutdown();
    }

    @Override
    public CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    var r = client.execute(requestBuilder.apply(batch));
                    var entity = r.getEntity();
                    return new LokiResponse(
                        r.getStatusLine().getStatusCode(),
                        entity != null ? EntityUtils.toString(entity) : "");
                } catch (Exception e) {
                    throw new RuntimeException("Error while sending batch to Loki", e);
                }
            }, httpThreadPool);
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setConnectionKeepAliveMs(long connectionKeepAliveMs) {
        this.connectionKeepAliveMs = connectionKeepAliveMs;
    }

    public void setHttpThreads(int httpThreads) {
        this.httpThreads = httpThreads;
    }

}
