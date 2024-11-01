---
id: configuration
title: Loki4j configuration
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
|dropRateLimitedBatches|false|If true, batches that Loki responds to with a `429` status code (TooManyRequests) will be dropped rather than retried|
|internalQueuesCheckTimeoutMs|25|A timeout for Loki4j threads to sleep if encode or send queues are empty. Decreasing this value means lower latency at the cost of higher CPU usage|
|useDirectBuffers|true|Use off-heap memory for storing intermediate data|
|drainOnStop|true|If true, the appender will try to send all the remaining events on shutdown, so the proper shutdown procedure might take longer. Otherwise, the appender will drop the unsent events|
|metricsEnabled|false|If true, the appender will report its metrics using Micrometer|
|verbose|false|If true, the appender will print its own debug logs to stderr|

### HTTP settings

Loki4j uses Java HTTP client by default.
If you want to switch to Apache HTTP client, please see [this](apacheclient.md) page.
HTTP settings are the same for both Java And Apache clients:

|Setting|Default|Description|
|-------|-------|-----------|
|http.url||**Required**. Loki endpoint to be used for sending batches|
|http.connectionTimeoutMs|30000|Time in milliseconds to wait for HTTP connection to Loki to be established before reporting an error|
|http.requestTimeoutMs|5000|Time in milliseconds to wait for HTTP request to Loki to be responded to before reporting an error|
|http.auth.username||Username to use for basic auth|
|http.auth.password||Password to use for basic auth|
|http.tenantId||Tenant identifier. It is required only for sending logs directly to Loki operating in multi-tenant mode. Otherwise, this setting has no effect|

### Format settings

By default, Loki4j encodes log record batches to JSON before sending them to Loki API.
If you want to use Protobuf encoding instead, please follow [this](protobuf.md) guide.
Format settings do not depend on the encoding you use.

|Setting|Default|Description|
|-------|-------|-----------|
|format.label.pattern||**Required**. Logback pattern to use for log record's label|
|format.label.pairSeparator|,|Character sequence to use as a separator between labels|
|format.label.keyValueSeparator|=|Character to use as a separator between label's name and its value|
|format.label.readMarkers|false|If true, Loki4j scans each log record for the attached LabelMarker to add its values to the record's labels|
|format.label.streamCache|BoundAtomicMapCache|An implementation of a stream cache to use. By default, caches up to 1000 unique label sets|
|format.staticLabels|false|If you use only a constant label set (e.g., same keys and values) for all log records, you can set this flag to true and save some CPU time on grouping records by label|
|format.sortByTime|false|If true, log records in batch are sorted by timestamp. If false, records will be sent to Loki in arrival order. Enable this if you see an 'entry out of order' error from Loki|

#### Plain text message layout

Plain text log message layout (backed by Logback's `PatternLayout` class) is used by default.
It supports the following settings:

|Setting|Default|Description|
|-------|-------|-----------|
|format.message.pattern||**Required**. Logback pattern to use for log record's message|

#### JSON message layout

This layout converts log messages to JSON.
Please check the [instruction](jsonlayout.md) on how to enable and use it.
This layout has the following settings:

|Setting|Default|Description|
|-------|-------|-----------|
|format.message.loggerName.enabled|true|Enable loggerName provider|
|format.message.loggerName.fieldName|logger_name|A JSON field name to use for loggerName|
|format.message.loggerName.targetLength|-1|The desired target length of logger name: `-1` to disable abbreviation, `0` to print class name only, >`0` to abbreviate to the target length|
|format.message.logLevel.enabled|true|Enable logLevel provider|
|format.message.logLevel.fieldName|level|A JSON field name to use for logLevel|
|format.message.mdc.enabled|true|Enable MDC provider|
|format.message.mdc.fieldName|mdc_|A prefix added to each JSON field name written by this provider|
|format.message.mdc.include||A set of MDC keys to include in JSON payload. If not specified, all keys are included|
|format.message.mdc.exclude||A set of MDC keys to exclude from JSON payload. The exclude list has precedence over the include list. If not specified, all keys are included|
|format.message.message.enabled|true|Enable message provider|
|format.message.message.fieldName|message|A JSON field name to use for message|
|format.message.stackTrace.enabled|true|Enable stackTrace provider|
|format.message.stackTrace.fieldName|stack_trace|A JSON field name to use for stackTrace|
|format.message.stackTrace.throwableConverter||An optional custom stack trace formatter|
|format.message.threadName.enabled|true|Enable threadName provider|
|format.message.threadName.fieldName|thread_name|A JSON field name to use for this threadName|
|format.message.timestamp.enabled|true|Enable timestamp provider|
|format.message.timestamp.fieldName|timestamp_ms|A JSON field name to use for timestamp|
|format.message.customProvider||An optional list of custom JSON providers|

## Examples

### Minimalistic zero-dependency configuration

In Java 11 and later, we can use standard HTTP client and Loki JSON API.
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

### Overriding the default settings

In this example, we would like to change max batch size to 100 records, batch timeout to 10s, label key-value separator to `:`,
and sort log records by time before sending them to Loki.
Messages should be in [JSON format](jsonlayout), without timestamp field, and with logger name abbreviated to 20 characters.
Also, we would like to use [Apache HTTP sender](apacheclient) with request timeout 10s and [Protobuf API](protobuf).
Finally, we want to see Loki4j debug output.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <batchMaxItems>100</batchMaxItems>
    <batchTimeoutMs>10000</batchTimeoutMs>
    <verbose>true</verbose>
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        <url>http://localhost:3100/loki/api/v1/push</url>
        <requestTimeoutMs>10000</requestTimeoutMs>
    </http>
    <format class="com.github.loki4j.logback.ProtobufEncoder">
        <label>
            <pattern>app:my-app,host:${HOSTNAME}</pattern>
            <keyValueSeparator>:</keyValueSeparator>
        </label>
        <message class="com.github.loki4j.logback.JsonLayout">
            <timestamp>
                <enabled>false</enabled>
            </timestamp>
            <loggerName>
                <targetLength>20</targetLength>
            </loggerName>
        </message>
        <sortByTime>true</sortByTime>
    </format>
</appender>
```
