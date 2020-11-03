package com.github.loki4j.logback;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
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
     * Max number of HTTP connections setting for HttpClient
     */
    private int maxConnections = 100;

    /**
     * Keep-alive setting for HttpClient
     */
    private boolean keepAlive = true;

    private CloseableHttpClient client;
    private Function<byte[], HttpPost> requestBuilder;

    @Override
    protected void startHttp(String contentType) {
        client = HttpClients
            .custom()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .setMaxConnTotal(maxConnections)
            .setDefaultSocketConfig(SocketConfig
                .custom()
                .setSoKeepAlive(keepAlive)
                .setSoTimeout((int)connectionTimeoutMs)
                .build())
            .setDefaultRequestConfig(RequestConfig
                .custom()
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
    
}
