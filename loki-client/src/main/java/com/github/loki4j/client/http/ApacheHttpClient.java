package com.github.loki4j.client.http;

import java.nio.ByteBuffer;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import java.util.concurrent.TimeUnit;


/**
 * Loki sender that is backed by Apache {@link org.apache.hc.client5.http.classic.HttpClient HttpClient}
 */
public final class ApacheHttpClient implements Loki4jHttpClient {

    private final HttpConfig conf;
    private final CloseableHttpAsyncClient client;
    private final SimpleRequestBuilder requestBuilder;

    /**
     * Buffer is needed for turning ByteBuffer into byte array
     * only if direct ByteBuffer arrived.
     */
    private byte[] bodyBuffer = new byte[0];

    public ApacheHttpClient(HttpConfig conf) {
        this.conf = conf;

        var cm = new PoolingAsyncClientConnectionManager();
        cm.setMaxTotal(conf.apache().maxConnections);
        cm.setDefaultMaxPerRoute(conf.apache().maxConnections);

        client = HttpAsyncClients
            .custom()
            .setConnectionManager(cm)
            .setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
                @Override
                public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
                    return TimeValue.of(conf.apache().connectionKeepAliveMs, TimeUnit.MILLISECONDS);
                }
            })
            .setDefaultRequestConfig(RequestConfig
                .custom()
                .setResponseTimeout(conf.connectionTimeoutMs, TimeUnit.MILLISECONDS)
                .setConnectTimeout(conf.connectionTimeoutMs, TimeUnit.MILLISECONDS)
                .setConnectionRequestTimeout(conf.requestTimeoutMs, TimeUnit.MILLISECONDS)
                .build())
            .build();

        requestBuilder = SimpleRequestBuilder
                .post(conf.pushUrl)
                .addHeader(HttpHeader.CONTENT_TYPE, conf.contentType);
        conf.tenantId.ifPresent(tenant -> requestBuilder.addHeader(HttpHeader.X_SCOPE_ORGID, tenant));
        conf.basicAuthToken().ifPresent(token -> requestBuilder.setHeader(HttpHeader.AUTHORIZATION, "Basic " + token));
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        if (batch.hasArray()) {
            requestBuilder.setBody(batch.array(), ContentType.parse(conf.contentType));
        } else {
            var len = batch.remaining();
            if (len > bodyBuffer.length)
                bodyBuffer = new byte[len];
            batch.get(bodyBuffer, 0, len);
            requestBuilder.setBody(bodyBuffer, ContentType.parse(conf.contentType));
        }

        var r = client.execute(requestBuilder.build(), new FutureCallback<>() {
            @Override
            public void completed(SimpleHttpResponse result) {

            }

            @Override
            public void failed(Exception ex) {
                throw new RuntimeException(ex);
            }

            @Override
            public void cancelled() {

            }
        });
        var response = r.get();
        return new LokiResponse(
                response.getCode(),
                response.getBodyText());
    }

    @Override
    public HttpConfig getConfig() {
        return conf;
    }
}
