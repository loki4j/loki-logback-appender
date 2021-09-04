package com.github.loki4j.logback;

import java.util.Optional;
import java.util.function.Function;

import com.github.loki4j.common.http.HttpConfig;
import com.github.loki4j.common.http.Loki4jHttpClient;
import com.github.loki4j.common.pipeline.PipelineConfig;

/**
 * Abstract class that implements a common logic shared between standard
 * HTTP sender configurators.
 */
public abstract class AbstractHttpSender implements HttpSender {

    public static final class BasicAuth {
        /**
         * Username to use for basic auth
         */
        String username;
        /**
         * Password to use for basic auth
         */
        String password;
        public void setUsername(String username) {
            this.username = username;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
    * Loki endpoint to be used for sending batches
    */
    private String url = "http://localhost:3100/loki/api/v1/push";

    /**
     * Tenant identifier.
     * It is required only for sending logs directly to Loki operating in multi-tenant mode.
     * Otherwise this setting has no effect
     */
    private Optional<String> tenantId = Optional.empty();

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    private long connectionTimeoutMs = 30_000;

    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    private long requestTimeoutMs = 5_000;

    /**
     * Optional creds for basic HTTP auth
     */
    private BasicAuth auth;

    protected void fillHttpConfig(HttpConfig.Builder builder) {
        builder
            .setPushUrl(url)
            .setTenantId(tenantId)
            .setConnectionTimeoutMs(connectionTimeoutMs)
            .setRequestTimeoutMs(requestTimeoutMs)
            .setUsername(Optional.ofNullable(auth).map(a -> a.username))
            .setPassword(Optional.ofNullable(auth).map(a -> a.password));
    }

    @Override
    public Function<HttpConfig, Loki4jHttpClient> getSenderFactory() {
        return PipelineConfig.defaultSenderFactory;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAuth(BasicAuth auth) {
        this.auth = auth;
    }

    public void setTenantId(String tenant) {
        this.tenantId = Optional.ofNullable(tenant);
    }

}
