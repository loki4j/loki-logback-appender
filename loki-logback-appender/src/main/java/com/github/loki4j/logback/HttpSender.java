package com.github.loki4j.logback;

import com.github.loki4j.common.http.HttpConfig;

/**
 * Basic interface that all Loki4j HTTP sender configurators must implement.
 */
public interface HttpSender {

    HttpConfig.Builder getConfig();

}
