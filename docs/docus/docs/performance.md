---
id: performance
title: Performance monitoring
sidebar_label: Monitoring
---

You can monitor Loki4j's performance by enabling an instrumentation powered by [Micrometer](https://micrometer.io/).

First, you need to make sure that Micrometer dependency is added to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.14.2</version>
</dependency>
```

<!--Gradle-->

```groovy
implementation 'io.micrometer:micrometer-core:1.14.2'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then, you need to enable metrics in your `logback.xml`:

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
loki4j.append.errors|Number of errors occurred while appending events
loki4j.encode.time|Time for a batch encode operation
loki4j.encode.events|Number of log events processed by encoder
loki4j.encode.batches|Number of batches processed by encoder
loki4j.encode.errors|Number of batches failed on encoding phase
loki4j.send.time|Time for a HTTP send operation
loki4j.send.bytes|Size of batches sent to Loki
loki4j.send.batches|Number of batches successfully sent to Loki
loki4j.send.errors|Number of batches not sent to Loki due to errors
loki4j.retry.errors|Number of failed attempts while sending batches to Loki
loki4j.drop.events|Number of events dropped due to backpressure settings
loki4j.unsent.events|Current number of encoded but not yet sent events
