package com.github.loki4j.logback;

import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.pipeline.PipelineConfig;

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
    public HttpConfig.Builder getConfig() {
        return PipelineConfig
            .apache(maxConnections, connectionKeepAliveMs)
            .fill(this::fillHttpConfig);
    }

}
