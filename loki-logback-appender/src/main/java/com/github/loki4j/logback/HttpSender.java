package com.github.loki4j.logback;

import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.http.Loki4jHttpClient;

/**
 * Basic interface that all Loki4j HTTP sender configurators must implement.
 */
public interface HttpSender {

    HttpConfig getConfig(String contentType);

    Loki4jHttpClient createHttpClient(HttpConfig config);

}
