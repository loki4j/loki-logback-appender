package com.github.loki4j.logback;

import java.util.function.Function;

import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.pipeline.PipelineConfig;

/**
 * A configurator for {@link com.github.loki4j.client.http.ApacheHttpClient ApacheHttpClient}
 */
public class ApacheHttpSender implements HttpSender {

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
    public HttpConfig.Builder getConfig() {
        return PipelineConfig.apache(maxConnections, connectionKeepAliveMs);
    }

    @Override
    public Function<HttpConfig, Loki4jHttpClient> getHttpClientFactory() {
        return PipelineConfig.defaultHttpClientFactory;
    }
}