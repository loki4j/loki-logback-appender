package com.github.loki4j.logback;

import com.github.loki4j.common.http.ApacheHttpClient;
import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.http.HttpConfig.ApacheHttpConfig;
import com.github.loki4j.common.http.Loki4jHttpClient;

/**
 * A configurator for {@link com.github.loki4j.common.http.ApacheHttpClient ApacheHttpClient}
 */
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

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public void setConnectionKeepAliveMs(long connectionKeepAliveMs) {
        this.connectionKeepAliveMs = connectionKeepAliveMs;
    }

    @Override
    public HttpConfig getConfig(String contentType) {
        return HttpConfig
            .builder(contentType)
            .fill(this::fillHttpConfig)
            .setApache(new ApacheHttpConfig(
                maxConnections,
                connectionKeepAliveMs))
            .build();
    }

    @Override
    public Loki4jHttpClient createHttpClient(HttpConfig config) {
        return new ApacheHttpClient(config);
    }
}
