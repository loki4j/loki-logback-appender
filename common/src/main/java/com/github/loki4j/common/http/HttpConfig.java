package com.github.loki4j.common.http;

import java.util.Base64;
import java.util.Optional;

public class HttpConfig {

    /**
    * Loki endpoint to be used for sending batches
    */
    public final String pushUrl = "http://localhost:3100/loki/api/v1/push";

    /**
     * Time in milliseconds to wait for HTTP connection to Loki to be established
     * before reporting an error
     */
    public final long connectionTimeoutMs = 30_000;

    /**
     * Time in milliseconds to wait for HTTP request to Loki to be responded
     * before reporting an error
     */
    public final long requestTimeoutMs = 5_000;

    /**
     * Tenant identifier.
     * It is required only for sending logs directly to Loki operating in multi-tenant mode.
     * Otherwise this setting has no effect
     */
    public final Optional<String> tenantId = Optional.empty();

    /**
     * Content-type header to send to Loki
     */
    String contentType;

    /**
     * Username to use for basic auth
     */
    public final Optional<String> username = Optional.empty();

    /**
     * Password to use for basic auth
     */
    public final Optional<String> password = Optional.empty();

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
    
}
