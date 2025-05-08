package com.github.loki4j.logback;

import java.util.function.Function;

import com.github.loki4j.client.http.HttpConfig;
import com.github.loki4j.client.http.Loki4jHttpClient;
import com.github.loki4j.client.pipeline.PipelineConfig;

/**
 * A configurator for {@link com.github.loki4j.client.http.JavaHttpClient JavaHttpClient}
 */
public class JavaHttpSender implements HttpSender {

    /**
     * Maximum time that excess idle threads will wait for new
     * tasks before terminating inner HTTP threads
     */
    private long innerThreadsExpirationMs = 5 * 60_000;

    public void setInnerThreadsExpirationMs(long innerThreadsExpirationMs) {
        this.innerThreadsExpirationMs = innerThreadsExpirationMs;
    }

    @Override
    public HttpConfig.Builder getConfig() {
        return PipelineConfig.java(innerThreadsExpirationMs);
    }

    @Override
    public Function<HttpConfig, Loki4jHttpClient> getHttpClientFactory() {
        return PipelineConfig.defaultHttpClientFactory;
    }
}
