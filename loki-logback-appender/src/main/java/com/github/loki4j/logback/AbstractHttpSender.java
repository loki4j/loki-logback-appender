package com.github.loki4j.logback;

import java.util.Base64;
import java.util.Optional;

import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract class that implements a common logic shared between standard
 * HTTP sender implementations
 */
public abstract class AbstractHttpSender extends ContextAwareBase implements HttpSender {

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
    protected String url = "http://localhost:3100/loki/api/v1/push";

    /**
     * Loki is a multi-tenant system; requests and data for tenant A are isolated from tenant B.
     * Requests to the Loki API should include an HTTP header (X-Scope-OrgID) that identifies the tenant for the request.
     * To run in multi-tenant mode, Loki should be started with auth_enabled: true.
     * Loki can be run in “single-tenant” mode where the X-Scope-OrgID header is not required. Due to that tenantId parameter is optional in HttpSender
     */
    protected Optional<String> tenantId = Optional.empty();

    /**
     * Content-type header to send to Loki
     */
    protected String contentType;

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    protected long connectionTimeoutMs = 30_000;

    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    protected long requestTimeoutMs = 5_000;

    /**
     * Optional creds for basic HTTP auth
     */
    private BasicAuth auth;

    /**
     * Token to pass to HTTP server if basic auth is enabled
     */
    protected Optional<String> basicAuthToken = Optional.empty();

    private boolean started = false;

    public void start() {
        if (auth != null) {
            // calculate auth token
            basicAuthToken = Optional.of(
                Base64
                    .getEncoder()
                    .encodeToString((auth.username + ":" + auth.password).getBytes())
            );
        }

        this.started = true;
    }

    public void stop() {
        this.started = false;
    }

    public boolean isStarted() {
        return started;
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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setTenantId(String tenant) {
        this.tenantId = Optional.ofNullable(tenant);
    }

}
