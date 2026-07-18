package com.github.loki4j.client.http;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.ByteBufferEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

/**
 * Loki sender that is backed by Apache
 * {@link org.apache.hc.client5.http.classic.HttpClient HttpClient5}
 */
public final class ApacheHttp5Client implements Loki4jHttpClient {

    private final HttpConfig conf;
    private final CloseableHttpClient client;
    private final Supplier<HttpPost> requestBuilder;

    public ApacheHttp5Client(HttpConfig conf) {
        this.conf = conf;

        var cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(conf.apache().maxConnections);
        cm.setDefaultMaxPerRoute(conf.apache().maxConnections);
        cm.setDefaultConnectionConfig(ConnectionConfig
                .custom()
                .setConnectTimeout(conf.connectionTimeoutMs, TimeUnit.MILLISECONDS)
                .build());

        client = HttpClients
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
                        .setConnectionRequestTimeout(conf.requestTimeoutMs, TimeUnit.MILLISECONDS)
                        .build())
                .build();

        requestBuilder = () -> {
            var request = new HttpPost(conf.pushUrl);
            request.addHeader(HttpHeader.CONTENT_TYPE, conf.contentType);
            conf.tenantId.ifPresent(tenant -> request.addHeader(HttpHeader.X_SCOPE_ORGID, tenant));
            conf.basicAuthToken().ifPresent(token -> request.setHeader(HttpHeader.AUTHORIZATION, "Basic " + token));
            return request;
        };
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        var request = requestBuilder.get();
        request.setEntity(new ByteBufferEntity(batch, ContentType.create(conf.contentType)));

        return client.execute(request, response -> new LokiResponse(
                response.getCode(),
                response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity())));
    }

    @Override
    public HttpConfig getConfig() {
        return conf;
    }
}
