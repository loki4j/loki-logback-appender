package com.github.loki4j.common.http;

import java.nio.ByteBuffer;

/**
 * Basic interface that all Loki4j HTTP senders must implement.
 */
public interface Loki4jHttpClient extends AutoCloseable {

    /**
     * Send a batch to Loki
     *
     * @return A response from Loki
     */
    public LokiResponse send(ByteBuffer batch);

}
