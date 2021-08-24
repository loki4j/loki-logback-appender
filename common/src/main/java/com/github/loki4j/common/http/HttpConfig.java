package com.github.loki4j.common.http;

import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Configuration properties for HTTP senders.
 */
public class HttpConfig {

    /**
    * Loki endpoint to be used for sending batches
    */
    public final String pushUrl;

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    public final long connectionTimeoutMs;

    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    public final long requestTimeoutMs;

    /**
     * Tenant identifier.
     * It is required only for sending logs directly to Loki operating in multi-tenant mode.
     * Otherwise this setting has no effect
     */
    public final Optional<String> tenantId;

    /**
     * Content-type header to send to Loki
     */
    public final String contentType;

    /**
     * Username to use for basic auth
     */
    public final Optional<String> username;

    /**
     * Password to use for basic auth
     */
    public final Optional<String> password;

    /**
     * Token to pass to HTTP server if basic auth is enabled
     */
    public final Optional<String> basicAuthToken() {
        return username.flatMap(u ->
            password.flatMap(p ->
                Optional.of(
                    Base64
                        .getEncoder()
                        .encodeToString((u + ":" + p).getBytes())
        )));
    }

    /**
     * A configuration specific to a certain HTTP client
     */
    public final ClientSpecificConfig clientSpecific;

    /**
     * A shortcut to preferences specific for {@link ApacheHttpClient ApacheHttpConfig}
     */
    public final ApacheHttpConfig apache() {
        return (ApacheHttpConfig)clientSpecific;
    }

    /**
     * A shortcut to preferences specific for {@link JavaHttpClient ApacheHttpConfig}
     */
    public final JavaHttpConfig java() {
        return (JavaHttpConfig)clientSpecific;
    }

    public HttpConfig(
            String pushUrl,
            long connectionTimeoutMs,
            long requestTimeoutMs,
            Optional<String> tenantId,
            String contentType,
            Optional<String> username,
            Optional<String> password,
            ClientSpecificConfig clientSpecific) {
        this.pushUrl = pushUrl;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.tenantId = tenantId;
        this.contentType = contentType;
        this.username = username;
        this.password = password;
        this.clientSpecific = clientSpecific;
    }

    public static Builder builder(String contentType) {
        return new Builder(contentType);
    }

    public static class Builder {
        public static final ApacheHttpConfig apache = new ApacheHttpConfig(1, 120_000);
        public static final JavaHttpConfig java = new JavaHttpConfig(5 * 60_000);

        private String contentType;

        private String pushUrl = "http://localhost:3100/loki/api/v1/push";
        private long connectionTimeoutMs = 30_000;
        private long requestTimeoutMs = 5_000;
        private Optional<String> tenantId = Optional.empty();
        private Optional<String> username = Optional.empty();
        private Optional<String> password = Optional.empty();
        private ClientSpecificConfig clientSpecific = java;

        public Builder(String contentType) {
            this.contentType = contentType;
        }

        public HttpConfig build() {
            return new HttpConfig(
                pushUrl,
                connectionTimeoutMs,
                requestTimeoutMs,
                tenantId,
                contentType,
                username,
                password,
                clientSpecific);
        }

        public Builder fill(Consumer<Builder> func) {
            func.accept(this);
            return this;
        }

        public Builder setPushUrl(String pushUrl) {
            this.pushUrl = pushUrl;
            return this;
        }

        public Builder setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
            return this;
        }

        public Builder setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
            return this;
        }

        public Builder setTenantId(Optional<String> tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder setUsername(Optional<String> username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(Optional<String> password) {
            this.password = password;
            return this;
        }

        public Builder setClientConfig(ClientSpecificConfig clientSpecificConfig) {
            this.clientSpecific = clientSpecificConfig;
            return this;
        }
    }

    public static interface ClientSpecificConfig { }

    public static class ApacheHttpConfig implements ClientSpecificConfig {
        /**
         * Maximum number of HTTP connections setting for HttpClient
         */
        public final int maxConnections;

        /**
         * A duration of time which the connection can be safely kept
         * idle for later reuse. This value should not be greater than
         * server.http-idle-timeout in your Loki config
         */
        public final long connectionKeepAliveMs;

        public ApacheHttpConfig(int maxConnections, long connectionKeepAliveMs) {
            this.maxConnections = maxConnections;
            this.connectionKeepAliveMs = connectionKeepAliveMs;
        }
    }

    public static class JavaHttpConfig implements ClientSpecificConfig {
        /**
         * Maximum time that excess idle threads will wait for new
         * tasks before terminating inner HTTP threads
         */
        public final long innerThreadsExpirationMs;

        public JavaHttpConfig(long innerThreadsExpirationMs) {
            this.innerThreadsExpirationMs = innerThreadsExpirationMs;
        }
    }
    
}
