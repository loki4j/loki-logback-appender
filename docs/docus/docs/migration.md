---
id: migration
title: Loki4j Migration Guide
sidebar_label: Migration Guide
---

## Upgrading from 1.3.x to 1.4.x

Version 1.4.0 contains several new features that may break the existing behavior for some users.
Please see below for the details.

#### Separate Protobuf JAR



#### Retry functionality added

Loki4j is designed to operate in presence of various errors and connection failures returned from Loki.
However, the previous versions tried to send each log batch only once, so all batches sent during
unavailability of Loki are lost.

In 1.4.0 Loki4j can try to send a log batch to Loki again, if the previous attempt failed.
Please note, that re-send is done only in case of `ConnectException` or `503` HTTP status from Loki.
All other exceptions as well as 4xx-5xx statuses are not retried in order to avoid duplicates.

#### Deprecated "batchSize" setting is removed

The `batchSize` setting was renamed to `batchMaxItems` back in 1.2.0, but you still could use the old name until 1.4.0.
Now the old name support was completely dropped, so please make sure you use `batchMaxItems` instead.


## Upgrading from 1.2.x to 1.3.x

Version 1.3.0 was focused on internal refactoring and bug fixing.
For most users 1.3.0 could be used as a drop-in replacement of 1.2.0.

The only breaking change that could affect these who use Loki4j performance metrics
is that the tag `host` is no longer hardcoded for all the reported Loki4j metrics.
This hardcoding was redundant as you always can set up `host` tag for any metric in your custom
Micrometer config on the application level.


## Upgrading from 1.1.x to 1.2.x

The development for version 1.2.0 was mostly concentrated around performance and memory usage.
Let's discuss how these changes affect the configuration in `logback.xml`.

#### Limitation for a batch length (in bytes) added

Previously there was no option to limit a batch size in bytes, and in case of large log messages this could
lead to `grpc: received message larger than max` error from Loki followed by the drop of this entire batch.

In 1.2.0 you can control the batch size using a new property `batchMaxBytes`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <batchMaxBytes>4194304</batchMaxBytes>
</appender>
```

Important note.
Loki limits max message size in bytes by comparing its size in uncompressed Protobuf format to a
value of setting `grpc_server_max_recv_msg_size`. That's why Loki4j's `batchMaxBytes`
setting should be less or equal than Loki's `grpc_server_max_recv_msg_size` setting (by default it's 4 MB).

The above statement also means that Loki4j batching based on `batchMaxBytes` does not depend
on the format Loki4j sends a batch in (JSON, compressed Protobuf). The _real_ size of HTTP body
can vary. Loki4j only tries to estimate the size of the batch as if it was in uncompressed Protobuf
format (without actually encoding it), and make sure this approximate size never to be less than
an actual size as counted by Loki.

#### `batchSize` property renamed to `batchMaxItems`

After introducing new `batchMaxBytes` setting (see the previous section), the existing
`batchSize` setting was renamed to `batchMaxItems` for consistency.

If you used `batchSize` in you configuration for previous versions of Loki4j, make sure
you've change it to `batchMaxItems` while upgrading to 1.2.0.

#### Switch to non-blocking and non-allocating send queue

In version 1.1.0 the backpressure mechanism was introduced. Sender queue was based on `ArrayBlockingQueue` and it
could be limited only by max number of batches in the queue using `sendQueueSize` setting.

In 1.2.0 this implementation was replaced by non-blocking `ConcurrentLinkedQueue` of `ByteBuffers`.
Now the sender queue could be limited by max size in bytes, so you now can control the memory consumption
more precisely. `sendQueueSize` setting is replaced with `sendQueueMaxBytes`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <sendQueueMaxBytes>41943040</sendQueueMaxBytes>
</appender>
```

#### Encoders and send queue now use ByteBuffers

This means that now you can switch between off-heap (i.e. direct) and on-heap buffers:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <useDirectBuffers>true</useDirectBuffers>
</appender>
```

## Upgrading from 1.0.x to 1.1.x

Version 1.1.0 introduces several changes to how the appender behaves and how it is configured in `logback.xml`.

#### Backpressure mechanism added

Before 1.1.0 if the appender sended log batches to Loki slower than they are produced by the application,
unsent batches were accumulated in the sender's execution queue. This led to unpredictable memory consumption and OOMs.

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