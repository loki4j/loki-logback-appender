---
id: migration
title: Loki4j migration guide
sidebar_label: Migration Guide
---

## Upgrading from 1.6.x to 2.0.x

Version 2.0.0 introduces significant changes to how the appender is configured in `logback.xml`.

#### New sub-section `<batch>`

In v2.0.0 all settings related to grouping log records into batches were moved to a separate section `<batch>`.
Previously these settings were in the "general" section (i.e., top-level `<appender>` settings).

Below is the list of settings moved to `<batch>` section:

|Old setting|New setting|
|-----------|-----------|
|batchMaxItems|batch.maxItems|
|batchMaxBytes|batch.maxBytes|
|batchTimeoutMs|batch.timeoutMs|
|sendQueueMaxBytes|batch.sendQueueMaxBytes|
|internalQueuesCheckTimeoutMs|batch.internalQueuesCheckTimeoutMs|
|useDirectBuffers|batch.useDirectBuffers|
|drainOnStop|batch.drainOnStop|
|format.staticLabels|batch.staticLabels|

If your v1.6.x configuration defines any of the old settings listed above, please move them to `<batch>` section with the proper new name.
The default values for these settings remain the same.

#### Some general settings moved to sub-section `<http>`

General settings related to sending batches to Loki via HTTP were moved to `<http>` section:

|Old setting|New setting|
|-----------|-----------|
|maxRetries|http.maxRetries|
|minRetryBackoffMs|http.minRetryBackoffMs|
|maxRetryBackoffMs|http.maxRetryBackoffMs|
|maxRetryJitterMs|http.maxRetryJitterMs|
|dropRateLimitedBatches|http.dropRateLimitedBatches|

If your v1.6.x configuration defines any of the old settings listed above, please move them to `<http>` section.
The default values for these settings remain the same.

#### Sub-section `<format>` removed

Settings from the `<format>` section were either removed or moved to "general" section (i.e., top-level `<appender>` settings):

|Old setting|New setting|
|-----------|-----------|
|format.label.pattern|labels|
|format.label.structuredMetadataPattern|structuredMetadata|
|format.message.*|message.*|
|format.label.pairSeparator|\<no longer configurable\>|
|format.label.keyValueSeparator|\<no longer configurable\>|
|format.label.readMarkers|readMarkers|
|format.label.streamCache|\<removed\>|
|format.staticLabels|batch.staticLabels|

If your v1.6.x configuration defines any of the old settings listed above, please move them to the top level.

Please note, that some of these settings have also changed their default values:

|Setting|Old default|New default|
|-------|-----------|-----------|
|labels|level=%level,host=${HOSTNAME}|agent=loki4j<br/>host=${HOSTNAME}|
|structuredMetadata|off|level=%level<br/>thread=%thread<br/>logger=%logger|
|message.pattern|l=%level c=%logger{20} t=%thread %msg %ex|\[%thread\] %logger{20} - %msg%n|

If you are not happy with the new default values, feel free to override them in you config.

#### Key-value pairs in labels and structured metadata now separated by new line

Previously Loki4j offered a setting `format.label.pairSeparator` that was `,` (comma) by default.
In v2.0.0 this separator is `\n` (new line), `\r` (carriage return), or any combination of them.
And it's no longer configurable.

For example, you would need to change your old-style labels and structured metadata configuration from:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <format>
        <label>
            <pattern>
                app=my-app, host=${HOSTNAME}
            </pattern>
            <structuredMetadataPattern>
                level=%level, thread=%thread
            </structuredMetadataPattern>
        </label>
        ...
    </format>
</appender>
```

to the new-style configuration (please note there are no commas!):

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <labels>
        app=my-app
        host=${HOSTNAME}
    </labels>
    <structuredMetadata>
        level=%level
        thread=%thread
    </structuredMetadata>
    ...
</appender>
```

#### "http.useProtobufApi" setting for switching to Loki Protobuf API

Previously, in order to switch from JSON to Protobuf flavor of Loki API, you would specify `ProtobufEncoder` class in the `format` section.

In v2.0.0 this was replaced by `http.useProtobufApi` flag:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <http>
        <useProtobufApi>true</useProtobufApi>
    </http>
</appender>
```

#### "http.sender" for switching between HTTP client implementations

Previously, in order to switch from Java to Apache HTTP client, you would specify `ApacheHttpSender` class in the `http` section.

In v2.0.0 this is now configured in `http.sender` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <sender class="com.github.loki4j.logback.ApacheHttpSender">
            ...
        </sender>
    </http>
    ...
</appender>
```

#### Logback version switched to 1.5.x

If your project depends on other external Logback appenders, please make sure all of them are compatible with Logback v1.5.x before upgrading.

## Upgrading from 1.5.x to 1.6.x

#### No Java 8 support

Starting from v1.6.0, Loki4j no longer provides `-jdk8` artifact.
Please upgrade your project at least to Java 11 before switching to Loki4j v1.6.0.

#### Logback version switched to 1.4.x

If your project depends on other external Logback appenders, please make sure all of them are compatible with Logback v1.4.x before upgrading.

#### loki-protobuf version updated to 0.0.2

Loki `.proto` files were updated to the latest version from the upstream.
If you use protobuf API for sending logs to Loki, please switch to loki-protobuf v0.0.2.

#### Minimal supported Loki version is 2.8.0

Loki v2.8.0 is the first version that supports label drop functionality that is now mandatory for Loki4j's integration tests.
Because of that Loki v1.6.1 was excluded from the integration tests and the compatibility matrix was updated.
However, in most cases Loki4j should still work fine with versions prior to 2.8.0.

#### "sortByTime" format setting removed

Since Loki v2.4.0 'entry out of order' is no longer an issue.
Now that minimal supported Loki version is 2.8.0, there is no point in keeping `format.sortByTime` setting.
Furthermore, having this property set to `true` in some cases might impose negative effect on performance.
That's why `format.sortByTime` is removed in v1.6.0.

#### Regex pair separators deprecated

Now you can use multiline strings to declare your key-value pairs in label and structured metadata patterns.
From now on, this will work even if the pair separator is a character (by default it's `,`) or a literal string.
A prefix `regex:` is still supported in `format.label.pairSeparator` for compatibility, but it might be removed in future versions.

#### "nopex" label setting removed

Previously `format.label.nopex` was used to suppress exception output into label pattern.
In v1.6.0 we have re-worked label formatting code, so that this setting is no longer needed.

#### "mdc.fieldName" setting replaced with "mdc.prefix"

JSON message layout setting `format.message.mdc.fieldName` is replaced with `format.message.mdc.prefix` as it better reflects its semantics.

## Upgrading from 1.4.x to 1.5.x

The most significant breaking change in Loki4j v1.5.0 is an upgrade to Logback v1.3.x.

#### Logback 1.2.x is no longer supported

The minimum supported version of Logback is now 1.3.0.
This version is not compatible with Logback v1.2.x series.
If your project depends on other external Logback appenders, please make sure all of them are compatible with Logback v1.3.x before upgrading.

#### Retry functionality changed

Previously, you could set only a constant value for a timeout between batch send retries.
Now you can switch between constant and exponential backoff using two new settings, `minRetryBackoffMs` and `maxRetryBackoffMs`.
If both of them have the same value, this value will be used as a constant timeout.
Otherwise, the timeout value will exponentially grow on each retry from `minRetryBackoffMs` to `maxRetryBackoffMs`.
The previous setting, `retryTimeoutMs`, was removed.

Please note that by default retry delays are exponential now, starting from 0.5s (1s, 2s, 4s, etc.).
Previously, by default, there was a constant delay 60s.

Also, in 1.5.0 a jitter (i.e., a small random variation) is added to the retry backoff delay.
You can set an upper bound for a jitter value using a new setting, `maxRetryJitterMs`.

Along with previously existing retry on status `503` received from Loki, in this version, Loki4j will, by default, retry sending batches after receiving `429` as well.
You can turn this off using the `dropRateLimitedBatches` setting.


## Upgrading from 1.3.x to 1.4.x

Version 1.4.0 contains several new features that may break the existing behavior for some users.
Please see below for the details.

#### Separate Protobuf JAR

If you use Protobuf format, now you need to add a new dependency to your project:

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-protobuf</artifactId>
    <version>0.0.1_pbX.Y.0</version>
</dependency>
```

A part `_pbX.Y.0` in the version means you can now use any supported PB version by substituting it here.
E.g. for Protobuf v3.21.x it should be `_pb3.21.0`.

In previous versions of Loki4j, you were required to add `protobuf-java` and `snappy-java` as dependencies to your project.
In 1.4.0, it's no longer required as the proper versions of these libs come as transitive dependencies of `loki-protobuf`.

#### Retry functionality added

Loki4j is designed to operate in the presence of various errors and connection failures returned from Loki.
However, the previous versions tried to send each log batch only once, so all batches sent during
the unavailability of Loki are lost.

In 1.4.0, Loki4j can try to send a log batch to Loki again if the previous attempt failed.
Please note that re-send is done only in case of `ConnectException` or `503` HTTP status from Loki.
All other exceptions, as well as 4xx-5xx statuses, are not retried to avoid duplicates.

#### Deprecated "batchSize" setting is removed

The `batchSize` setting was renamed to `batchMaxItems` back in 1.2.0, but you could still use the old name until 1.4.0.
Now, the old name support has completely dropped, so please make sure you use `batchMaxItems` instead.


## Upgrading from 1.2.x to 1.3.x

Version 1.3.0 was focused on internal refactoring and bug fixing.
For most users, 1.3.0 could be used as a drop-in replacement of 1.2.0.

The only breaking change that could affect those who use Loki4j performance metrics
is that the tag `host` is no longer hardcoded for all the reported Loki4j metrics.
This hardcoding was redundant as you can always set up a `host` tag for any metric in your custom
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