package com.github.loki4j.logback;

import com.github.loki4j.common.LokiResponse;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;

/**
 * Basic interface that all Loki4j HTTP senders must implement.
 *
 * Each implementation must be annotated with {@link ch.qos.logback.core.joran.spi.NoAutoStart}
 */
public interface HttpSender extends ContextAware, LifeCycle {

    /**
     * Send a batch to Loki
     *
     * @return A response from Loki
     */
    public LokiResponse send(byte[] batch);

    /**
     * Get Loki target URL
     */
    public String getUrl();

    /**
     * Set Loki target URL
     */
    public void setUrl(String url);

    /**
     * Set optional tenantId
     * @param tenantId
     */
    public void setTenantId(String tenantId);

    /**
     * Content type of the requests to send to Loki.
     * It depends on the encoder selected in config (JSON vs Protobuf)
     */
    public void setContentType(String contentType);

}
