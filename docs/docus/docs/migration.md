---
id: migration
title: Loki4j Migration Guide
sidebar_label: Migration Guide
---

## Upgrading from 1.0.x to 1.1.x

Version 1.1.0 introduces several changes to how the appender behaves and how it is configured in `logback.xml`.

#### Backpressure mechanism added

Before 1.1.0 if the appender sends log batches to Loki slower than they are produced by the application,
unsent batches are accumulated in the sender's execution queue. This led to unpredictable memory consumption and OOMs.

Version 1.1.0 introduces an explicit setting for sender's queue size. Now any extra log records arrived being dropped
without encoding or any other processing. You can change this setting in the `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <sendQueueSize>50000</sendQueueSize>
</appender>
```

Dropped events are logged in Loki4j log (stderr) and reported via `loki4j.drop.events` metric.
If you see events are dropped in spikes, consider increasing `sendQueueSize`.

#### Drain on stop behavior changed

Loki4j has graceful shutdown procedure: when `stop()` is called it stops to accept new log events
and tries to drain unprocessed log events, so they are sent to Loki before processing pipeline is stopped.

Before 1.1.0 the appender waited for drain to complete for 500ms and then used to shutdown the
processing pipeline anyway.

Now this behavior is configurable using `drainOnStop` setting. If this setting is enabled the appender
will wait until all unsent events are processed whatever it takes. This is the default option.
You can disable this behavior in your `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <drainOnStop>false</drainOnStop>
</appender>
```

In this case the appender will shutdown immediately, all unsent events will be lost.

#### Metrics configuration changed

Previously it was required to switch to `InstrumentedLoki4jAppender` in order to enable the metrics.
Now it's done using `metricsEnabled` setting in `Loki4jAppender`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <metricsEnabled>true</metricsEnabled>
</appender>
```

`InstrumentedLoki4jAppender` does not exist anymore.

#### ThreadPool-related settings are removed

Since 1.1.0 Loki4j uses 3 dedicated threads for operating (encoder, sender, and scheduler).
Thus, the following settings were removed:

- processingThreads
- http.httpThreads

## Upgrading from 0.4.x to 1.0.x

Version 1.0.0 introduces several significant changes to how the appender is configured in `logback.xml`.

#### Appender class name changed

In 0.4.x there were two appender classes `LokiJavaHttpAppender` and `LokiApacheHttpAppender` depending on which
underlying HTTP client should be used for sending data to Loki.
In 1.0.x HTTP client setting is pushed down to a separate section (see below) so there is only one main appender class:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
```

#### New sub-section `<http>`

In 1.0.x all HTTP-related settings were moved to a separate section `<http>`.
You can choose between Java- or Apache-backed HTTP sender via `class` attribute of `<http>`:

```xml
<http class="com.github.loki4j.logback.JavaHttpSender">
```

or

```xml
<http class="com.github.loki4j.logback.ApacheHttpSender">
```

If `class` is not specified `JavaHttpSender` is used.
Please note that only `ApacheHttpSender` is available for Java 8.

Below is the list of settings moved to `<http>` section (previously they were in `<appender>` section):

- url
- connectionTimeoutMs
- requestTimeoutMs

`ApacheHttpSender`-specific settings:

- maxConnections
- connectionKeepAliveMs
- httpThreads

#### Encoder sub-section renamed to `<format>`

In 1.0.0 `<encoder>` section was renamed to `<format>`.
Encoder classes and settings names for this section remain unchanged.

```xml
<format class="com.github.loki4j.logback.JsonEncoder">
```

or

```xml
<format class="com.github.loki4j.logback.ProtobufEncoder">
```

If `class` is not specified `JsonEncoder` is used.

#### Example

Given the following 0.4.x config:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiApacheHttpAppender">
    <url>http://localhost:3100/loki/api/v1/push</url>
    <requestTimeoutMs>15000</requestTimeoutMs>
    <batchSize>100</batchSize>
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </encoder>
</appender>
```

we can re-write it for 1.0.0:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <batchSize>100</batchSize>
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        <url>http://localhost:3100/loki/api/v1/push</url>
        <requestTimeoutMs>15000</requestTimeoutMs>
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