package com.github.loki4j.logback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import com.github.loki4j.common.HttpHeaders;
import com.github.loki4j.common.LokiResponse;

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

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * Loki sender that is backed by Apache {@link org.apache.http.client.HttpClient HttpClient}
 */
@NoAutoStart
public class ApacheHttpSender extends AbstractHttpSender {

    /**
     * Maximum number of HTTP connections setting for HttpClient
     */
    private int maxConnections = 1;

    /**
     * A duration of time which the connection can be safely kept
     * idle for later reuse. This value should not be greater than
     * server.http-idle-timeout in your Loki config
     */
    private long connectionKeepAliveMs = 120_000;

    private CloseableHttpClient client;
    private Supplier<HttpPost> requestBuilder;

    /**
     * Buffer is needed for turning ByteBuffer into byte array
     * only if direct ByteBuffer arrived.
     */
    private byte[] bodyBuffer = new byte[0];

    @Override
    public void start() {
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

        requestBuilder = () -> {
            var request = new HttpPost(url);
            request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
            tenantId.ifPresent(tenant -> request.addHeader(HttpHeaders.X_SCOPE_ORGID, tenant));
            basicAuthToken.ifPresent(token -> request.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + token));
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
    }

    @Override
    public LokiResponse send(ByteBuffer batch) {
        try {
            var request = requestBuilder.get();
            if (batch.hasArray()) {
                request.setEntity(new ByteArrayEntity(batch.array(), batch.position(), batch.remaining()));
            } else {
                var len = batch.remaining();
                if (len > bodyBuffer.length)
                    bodyBuffer = new byte[len];
                batch.get(bodyBuffer, 0, len);
                request.setEntity(new ByteArrayEntity(bodyBuffer, 0, len));
            }

            var r = client.execute(request);
            var entity = r.getEntity();
            return new LokiResponse(
                r.getStatusLine().getStatusCode(),
                entity != null ? EntityUtils.toString(entity) : "");
        } catch (Exception e) {
            throw new RuntimeException("Error while sending batch to Loki", e);
        }
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setConnectionKeepAliveMs(long connectionKeepAliveMs) {
        this.connectionKeepAliveMs = connectionKeepAliveMs;
    }

}
