---
id: performance
title: Performance Monitoring
sidebar_label: Performance Monitoring
---

You can monitor Loki4j's performance by enabling instrumentation powered by [Micrometer](https://micrometer.io/).

First you need to make sure that Micrometer dependency added to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.6.4</version>
</dependency>
```

<!--Gradle-->

```groovy
compile 'io.micrometer:micrometer-core:1.6.4'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you need to enable metrics in your `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <metricsEnabled>true</metricsEnabled>
</appender>
```

You will be able to monitor the following Loki4j metrics:

Metric|Description
-------|-------
loki4j.append.time|Time for a single event append operation
loki4j.encode.time|Time for a batch encode operation
loki4j.encode.events|Number of log events processed by encoder
loki4j.encode.batches|Number of batches processed by encoder
loki4j.send.time|Time for a HTTP send operation
loki4j.send.bytes|Size of batches sent to Loki
loki4j.send.batches|Number of batches sent to Loki
loki4j.send.errors|Number of errors occurred while sending batches to Loki
loki4j.drop.events|Number of events dropped due to backpressure settings

Enable `InstrumentedLoki4jAppender` only if you intend to monitor Loki4j's performance metrics.
It is not recommended to use instrumentation on regular basis.