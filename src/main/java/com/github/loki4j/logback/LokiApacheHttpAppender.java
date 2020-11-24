package com.github.loki4j.logback;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

/**
 * Loki appender that is backed by Apache {@link org.apache.http.client.HttpClient HttpClient}
 */
public class LokiApacheHttpAppender extends AbstractLoki4jAppender {

    /**
     * Maximum number of HTTP connections setting for HttpClient
     */
    private int maxConnections = 1;

    /**
     * Maximum life span of persistent connections for HttpClient
     */
    private long connectionTtlMs = 30_000;

    private CloseableHttpClient client;
    private Function<byte[], HttpPost> requestBuilder;

    @Override
    protected void startHttp(String contentType) {
        var cm = new PoolingHttpClientConnectionManager(connectionTtlMs, TimeUnit.MILLISECONDS);
        cm.setMaxTotal(maxConnections);
        cm.setDefaultMaxPerRoute(maxConnections);

        client = HttpClients
            .custom()
            .setConnectionManager(cm)
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
    }

    @Override
    protected void stopHttp() {
        try {
            client.close();
        } catch (IOException e) {
            addWarn("Error while closing Apache HttpClient", e);
        }
    }

    @Override
    protected CompletableFuture<LokiResponse> sendAsync(byte[] batch) {
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

    public void setConnectionTtlMs(long connectionTtlMs) {
        this.connectionTtlMs = connectionTtlMs;
    }

}
