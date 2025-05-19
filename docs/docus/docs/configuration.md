---
id: configuration
title: Loki4j configuration
sidebar_label: Configuration
---

## Reference

This page describes an XML configuration of Loki4j appender.
Most of the settings listed below have reasonable defaults and, thus, are optional.
These few that are required are marked explicitly.

### General settings

|Setting|Default|Description|
|-------|-------|-----------|
|labels|agent=loki4j<br/>host=${HOSTNAME}|Labels for a log record. A list of key-value pairs separated by a new line. Each value should be a valid Logback or Loki4j pattern|
|structuredMetadata|level=%level<br/>thread=%thread<br/>logger=%logger|Structured metadata for a log record. A list of key-value pairs separated by a new line. Each value should be a valid Logback or Loki4j pattern. Use pattern "off" to disable structured metadata generation|
|readMarkers|false|If true, Loki4j scans each log record for the attached SLF4J marker to add its values to the record's labels or structured metadata|
|metricsEnabled|false|If true, the appender will report its metrics using Micrometer|
|verbose|false|If true, the appender will print its own debug logs to stderr|

#### Plain text message layout

By default Loki4j uses plain text log message layout backed by Logback's `PatternLayout` class.
It supports the following settings:

|Setting|Default|Description|
|-------|-------|-----------|
|message.pattern|\[%thread\] %logger{20} - %msg%n|Logback pattern to use for log record's message|

#### JSON message layout

This layout converts log messages to JSON format.
Please check the [instruction](jsonlayout.md) on how to enable and use it.
This layout has the following settings:

|Setting|Default|Description|
|-------|-------|-----------|
|message.loggerName.enabled|true|Enable loggerName provider|
|message.loggerName.fieldName|logger_name|A JSON field name to use for loggerName|
|message.loggerName.targetLength|-1|The desired target length of logger name: `-1` to disable abbreviation, `0` to print class name only, >`0` to abbreviate to the target length|
|message.logLevel.enabled|true|Enable logLevel provider|
|message.logLevel.fieldName|level|A JSON field name to use for logLevel|
|message.kvp.enabled|true|Enable keyValuePair provider|
|message.kvp.prefix|kvp_|A prefix added to each JSON field name written by this provider|
|message.kvp.noPrefix|false|Whether to omit prefix for this provider|
|message.kvp.fieldSerializer||An implementation of field JSON serializer. By default, `writeObjectField()` is used|
|message.kvp.include||A set of keys to include in JSON payload. If not specified, all keys are included|
|message.kvp.exclude||A set of keys to exclude from JSON payload. The exclude list has precedence over the include list. If not specified, all keys are included|
|message.mdc.enabled|true|Enable MDC provider|
|message.mdc.prefix|mdc_|A prefix added to each JSON field name written by this provider|
|message.mdc.noPrefix|false|Whether to omit prefix for this provider|
|message.mdc.include||A set of MDC keys to include in JSON payload. If not specified, all keys are included|
|message.mdc.exclude||A set of MDC keys to exclude from JSON payload. The exclude list has precedence over the include list. If not specified, all keys are included|
|message.message.enabled|true|Enable message provider|
|message.message.fieldName|message|A JSON field name to use for message|
|message.stackTrace.enabled|true|Enable stackTrace provider|
|message.stackTrace.fieldName|stack_trace|A JSON field name to use for stackTrace|
|message.stackTrace.throwableConverter||An optional custom stack trace formatter|
|message.threadName.enabled|true|Enable threadName provider|
|message.threadName.fieldName|thread_name|A JSON field name to use for this threadName|
|message.timestamp.enabled|true|Enable timestamp provider|
|message.timestamp.fieldName|timestamp_ms|A JSON field name to use for timestamp|
|message.customProvider||An optional list of custom JSON providers|

### HTTP settings

Loki4j uses standard Java HTTP client by default.
If you want to switch to Apache HTTP client, please see [this](apacheclient.md) page.
Below are the HTTP settings same for both Java and Apache clients:

|Setting|Default|Description|
|-------|-------|-----------|
|http.url||**Required**. Loki endpoint to be used for sending batches|
|http.auth.username||Username to use for basic auth|
|http.auth.password||Password to use for basic auth|
|http.tenantId||Tenant identifier. It is required only for sending logs directly to Loki operating in multi-tenant mode. Otherwise, this setting has no effect|
|http.connectionTimeoutMs|30000|Time in milliseconds to wait for HTTP connection to Loki to be established before reporting an error|
|http.requestTimeoutMs|5000|Time in milliseconds to wait for HTTP request to Loki to be responded to before reporting an error|
|http.maxRetries|2|Max number of attempts to send a batch to Loki before it will be dropped. A failed batch send could be retried in case of `ConnectException`, or receiving statuses `429`, `503` from Loki. All other exceptions and 4xx-5xx statuses do not cause a retry in order to avoid duplicates|
|http.minRetryBackoffMs|500|Initial backoff delay before the next attempt to re-send a failed batch. Batches are retried with an exponential backoff (e.g. 0.5s, 1s, 2s, 4s, etc.) and jitter|
|http.maxRetryBackoffMs|60000|Maximum backoff delay before the next attempt to re-send a failed batch|
|http.maxRetryJitterMs|500|Upper bound for a jitter added to the retry delays|
|http.dropRateLimitedBatches|false|If true, batches that Loki responds to with a `429` status code (TooManyRequests) will be dropped rather than retried|
|http.useProtobufApi|false|If true, Loki4j uses Protobuf Loki API instead of JSON|
|http.sender|JavaHttpSender|An implementation of HTTP sender to use|

### Batch settings

Before sending log records to Loki, the appender groups them into batches.
This section contains settings related to this process.

|Setting|Default|Description|
|-------|-------|-----------|
|batch.maxItems|1000|Max number of events to put into a single batch before sending it to Loki|
|batch.maxBytes|4194304|Max number of bytes a single batch can contain (as counted by Loki). This value should not be greater than `server.grpc_server_max_recv_msg_size` in your Loki config|
|batch.timeoutMs|60000|Max time in milliseconds to keep a batch before sending it to Loki, even if max items/bytes limits for this batch are not reached|
|batch.staticLabels|false|If true, labels will be calculated only once for the first log record and then used for all other log records without re-calculation. Otherwise, they will be calculated for each record individually|
|batch.drainOnStop|true|If true, the appender will try to send all the remaining events on shutdown, so the proper shutdown procedure might take longer. Otherwise, the appender will drop the unsent events|
|batch.sendQueueMaxBytes|41943040|Max number of bytes to keep in the send queue. When the queue is full, incoming log events are dropped|
|batch.internalQueuesCheckTimeoutMs|25|A timeout for Loki4j threads to sleep if encode or send queues are empty. Decreasing this value means lower latency at the cost of higher CPU usage|
|batch.useDirectBuffers|true|Use off-heap memory for storing intermediate data|


## Examples

### Minimalistic zero-dependency configuration

This setup is supported natively by Loki4j and does not require any extra dependencies.
We need to define only required settings, leaving optional settings with their default values.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
</appender>
```

### Overriding the default settings

In this example, we would like to see our application name as a label, structured metadata should be disabled.
Messages should be in [JSON format](jsonlayout), without timestamp field, and with logger name abbreviated to 20 characters.
We would like to use [Apache HTTP sender](apacheclient) with request timeout 10s and [Protobuf API](protobuf).
Also, max batch size should be 100 records and batch timeout - 10s.
Finally, we want to see Loki4j debug output.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <labels>
        app = my-app
        host = ${HOSTNAME}
    </labels>
    <structuredMetadata>off</structuredMetadata>
    <message class="com.github.loki4j.logback.JsonLayout">
        <timestamp>
            <enabled>false</enabled>
        </timestamp>
        <loggerName>
            <targetLength>20</targetLength>
        </loggerName>
    </message>
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
        <useProtobufApi>true</useProtobufApi>
        <sender class="com.github.loki4j.logback.ApacheHttpSender" />
        <requestTimeoutMs>10000</requestTimeoutMs>
    </http>
    <batch>
        <maxItems>100</batchMaxItems>
        <timeoutMs>10000</batchTimeoutMs>
    </batch>
    <verbose>true</verbose>
</appender>
```
