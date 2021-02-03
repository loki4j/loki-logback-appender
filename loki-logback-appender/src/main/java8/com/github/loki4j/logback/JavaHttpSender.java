package com.github.loki4j.logback;

import com.github.loki4j.common.LokiResponse;

import ch.qos.logback.core.joran.spi.NoAutoStart;

/**
 * A stub class for Java 8 that should fail on constructor.
 * 
 * JavaHttpSender is not implemented for Java 8,
 * only {@link com.github.loki4j.logback.ApacheHttpSender} should be used.
 * 
 * If this code is called, it means the configuration is incorrect,
 * so we throw an exception with a meaningful message here.
 */
@NoAutoStart
public class JavaHttpSender extends AbstractHttpSender {

    public JavaHttpSender() {
        // only possible when running under Java 8
        throw new RuntimeException(
            "JavaHttpSender is supported only for Java 11+. Please specify <http class=\"" +
            ApacheHttpSender.class.getName() +
            "\"> your logback config");
    }

    @Override
    public LokiResponse send(byte[] batch) {
        throw new IllegalStateException("Not implemented");
    }
}
