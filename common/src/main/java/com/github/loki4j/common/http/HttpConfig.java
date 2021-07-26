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
    public Optional<String> basicAuthToken() {
        return username.flatMap(u ->
            password.flatMap(p ->
                Optional.of(
                    Base64
                        .getEncoder()
                        .encodeToString((u + ":" + p).getBytes())
        )));
    }

    /**
     * Preferences specific to {@link ApacheHttpClient ApacheHttpConfig}
     */
    public final ApacheHttpConfig apache;

    /**
     * Preferences specific to {@link JavaHttpClient ApacheHttpConfig}
     */
    public final JavaHttpConfig java;


    public HttpConfig(
            String pushUrl,
            long connectionTimeoutMs,
            long requestTimeoutMs,
            Optional<String> tenantId,
            String contentType,
            Optional<String> username,
            Optional<String> password,
            ApacheHttpConfig apache,
            JavaHttpConfig java) {
        this.pushUrl = pushUrl;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.tenantId = tenantId;
        this.contentType = contentType;
        this.username = username;
        this.password = password;
        this.apache = apache;
        this.java = java;
    }

    public static Builder builder(String contentType) {
        return new Builder(contentType);
    }

    public static class Builder {
        private String contentType;

        private String pushUrl = "http://localhost:3100/loki/api/v1/push";
        private long connectionTimeoutMs = 30_000;
        private long requestTimeoutMs = 5_000;
        private Optional<String> tenantId = Optional.empty();
        private Optional<String> username = Optional.empty();
        private Optional<String> password = Optional.empty();
        private ApacheHttpConfig apache = new ApacheHttpConfig(1, 120_000);
        private JavaHttpConfig java = new JavaHttpConfig(5 * 60_000);

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
                apache,
                java);
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

        public Builder setApache(ApacheHttpConfig apache) {
            this.apache = apache;
            return this;
        }

        public Builder setJava(JavaHttpConfig java) {
            this.java = java;
            return this;
        }
    }

    public static class ApacheHttpConfig {
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

    public static class JavaHttpConfig {
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
