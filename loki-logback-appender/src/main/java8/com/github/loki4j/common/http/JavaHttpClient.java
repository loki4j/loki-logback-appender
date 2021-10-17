package com.github.loki4j.client.http;

import java.nio.ByteBuffer;

import com.github.loki4j.logback.ApacheHttpSender;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * A stub class for Java 8 that should fail on constructor.
 * 
 * JavaHttpClient is not implemented for Java 8,
 * only {@link com.github.loki4j.logback.ApacheHttpSender} should be used.
 * 
 * If this code is called, it means the configuration is incorrect,
 * so we throw an exception with a meaningful message here.
 */
@NoAutoStart
public class JavaHttpClient implements Loki4jHttpClient {

    public JavaHttpClient(HttpConfig conf) {
        // only possible when running under Java 8
        throw new RuntimeException(
            "JavaHttpSender is supported only for Java 11+. Please specify <http class=\"" +
            ApacheHttpSender.class.getName() +
            "\"> your logback config");
    }

    @Override
    public void close() throws Exception {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public LokiResponse send(ByteBuffer batch) throws Exception {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public HttpConfig getConfig() {
        throw new IllegalStateException("Not implemented");
    }
}
