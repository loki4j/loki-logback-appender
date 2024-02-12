---
id: configuration
title: Loki4j Configuration
sidebar_label: Configuration
---

## Reference

Loki4j appender XML configuration consists of 3 sections: general settings, HTTP settings, and format settings.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <!-- general settings -->
    <http>
        <!-- HTTP settings -->
    </http>
    <format>
        <!-- format settings -->
    </format>
</appender>
```

Most Loki4j appender settings are optional. These few that are required are marked explicitly.

### General settings

|Setting|Default|Description|
|-------|-------|-----------|
|batchMaxItems|1000|Max number of events to put into a single batch before sending it to Loki|
|batchMaxBytes|4194304|Max number of bytes a single batch can contain (as counted by Loki). This value should not be greater than `server.grpc_server_max_recv_msg_size` in your Loki config|
|batchTimeoutMs|60000|Max time in milliseconds to keep a batch before sending it to Loki, even if max items/bytes limits for this batch are not reached|
|sendQueueMaxBytes|41943040|Max number of bytes to keep in the send queue. When the queue is full, incoming log events are dropped|
|maxRetries|2|Max number of attempts to send a batch to Loki before it will be dropped. A failed batch send could be retried in case of `ConnectException`, or receiving statuses `429`, `503` from Loki. All other exceptions and 4xx-5xx statuses do not cause a retry in order to avoid duplicates|
|minRetryBackoffMs|500|Initial backoff delay before the next attempt to re-send a failed batch. Batches are retried with an exponential backoff (e.g. 0.5s, 1s, 2s, 4s, etc.) and jitter|
|maxRetryBackoffMs|60000|Maximum backoff delay before the next attempt to re-send a failed batch|
|maxRetryJitterMs|500|Upper bound for a jitter added to the retry delays|
|dropRateLimitedBatches|false|Disables retries of batches that Loki responds to with a 429 status code (TooManyRequests). This reduces impacts on batches from other tenants, which could end up being delayed or dropped due to backoff.|
|internalQueuesCheckTimeoutMs|25|A timeout for Loki4j threads to sleep if encode or send queues are empty. Decreasing this value means lower latency at cost of higher CPU usage|
|useDirectBuffers|true|Use off-heap memory for storing intermediate data|
|drainOnStop|true|If true, the appender will try to send all the remaining events on shutdown, so the proper shutdown procedure might take longer. Otherwise, the appender will drop the unsent events|
|metricsEnabled|false|If true, the appender will report its metrics using Micrometer|
|verbose|false|If true, the appender will print its own debug logs to stderr|

### HTTP settings

|Setting|Default|Description|
|-------|-------|-----------|
|http.url||**Required**. Loki endpoint to be used for sending batches|
|http.connectionTimeoutMs|30000|Time in milliseconds to wait for HTTP connection to Loki to be established before reporting an error|
|http.requestTimeoutMs|5000|Time in milliseconds to wait for HTTP request to Loki to be responded before reporting an error|
|http.auth.username||Username to use for basic auth|
|http.auth.password||Password to use for basic auth|
|http.tenantId||Tenant identifier. It is required only for sending logs directly to Loki operating in multi-tenant mode. Otherwise this setting has no effect|

### Format settings

|Setting|Default|Description|
|-------|-------|-----------|
|format.label.pattern||**Required**. Logback pattern to use for log record's label|
|format.label.pairSeparator|,|Character sequence to use as a separator between labels. If starts with "regex:" prefix, the remainder is applied as a regular expression separator. Otherwise, the provided char sequence is used as a separator literally|
|format.label.keyValueSeparator|=|Character to use as a separator between label's name and its value|
|format.label.readMarkers|false|If true, Loki4j scans each log record for attached LabelMarker to add its values to record's labels|
|format.label.nopex|true|If true, exception info is not added to labels. If false, you should take care of proper formatting|
|format.label.streamCache|UnboundAtomicMapCache|An implementation of a Stream cache to use|
|format.message.pattern||**Required**. Logback pattern to use for log record's message|
|format.staticLabels|false|If you use only one label for all log records, you can set this flag to true and save some CPU time on grouping records by label|
|format.sortByTime|false|If true, log records in batch are sorted by timestamp. If false, records will be sent to Loki in arrival order. Enable this if you see 'entry out of order' error from Loki|

## Examples

### Minimalistic zero-dependency configuration

In Java 11 and later we can use standard HTTP client and Loki JSON API.
This setup is supported natively by Loki4j and does not require any extra dependencies.
We need to define only required settings, leaving optional settings with their default values.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </format>
</appender>
```

### Minimalistic configuration compatible with Java 8

For Java 8 we would need to modify the previous example a bit.
We would have to use Apache HTTP sender because the default Java HTTP sender works only for Java 11+.
Check the corresponding [configuration section](apacheclient) for details.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
    <format>
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </format>
</appender>
```

### Overriding the default settings

In this example we would like to change max batch size to 100 records, batch timeout to 10s, label key-value separator to `:`,
and sort log records by time before sending them to Loki.
Also, we would like to use [Apache HTTP sender](apacheclient) with a pool of 10 connections and [Protobuf API](protobuf).
Finally, we want to see Loki4j debug output.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <batchMaxItems>100</batchMaxItems>
    <batchTimeoutMs>10000</batchTimeoutMs>
    <verbose>true</verbose>
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        <url>http://localhost:3100/loki/api/v1/push</url>
        <maxConnections>10</maxConnections>
    </http>
    <format class="com.github.loki4j.logback.ProtobufEncoder">
        <label>
            <pattern>app:my-app,host:${HOSTNAME}</pattern>
            <keyValueSeparator>:</keyValueSeparator>
        </label>
        <message>
            <pattern>l=%level c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
        <sortByTime>true</sortByTime>
    </format>
</appender>
```
