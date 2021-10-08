package com.github.loki4j.logback;

import java.util.function.Function;

import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.http.Loki4jHttpClient;

/**
 * Basic interface that all Loki4j HTTP sender configurators must implement.
 */
public interface HttpSender {

    HttpConfig.Builder getConfig();

    Function<HttpConfig, Loki4jHttpClient> getHttpClientFactory();

}
