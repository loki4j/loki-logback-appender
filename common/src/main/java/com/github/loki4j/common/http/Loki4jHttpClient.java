package com.github.loki4j.common.http;

import java.nio.ByteBuffer;

/**
 * Basic interface that all Loki4j HTTP senders must implement.
 */
public interface Loki4jHttpClient extends AutoCloseable {

    /**
     * Get HTTP configuration for this client
     */
    public HttpConfig getConfig();

    /**
     * Send a batch to Loki
     *
     * @return A response from Loki
     * @throws Exception If send was not successful
     */
    public LokiResponse send(ByteBuffer batch) throws Exception;

}
