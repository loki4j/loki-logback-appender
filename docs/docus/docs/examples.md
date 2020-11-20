---
id: examples
title: Configuration Examples
sidebar_label: Examples
---

Example configuration using `LokiJavaHttpAppender` and `JsonEncoder`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <url>http://localhost:3100/loki/api/v1/push</url>
    <batchSize>1000</batchSize>
    <batchTimeoutMs>10000</batchTimeoutMs>
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <label>
            <pattern>app=my-app,host=${HOSTNAME}</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
        <sortByTime>true</sortByTime>
        <staticLabels>true</staticLabels>
    </encoder>
</appender>
```

Example configuration using `LokiApacheHttpAppender` and `ProtobufEncoder`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiApacheHttpAppender">
    <url>http://localhost:3100/loki/api/v1/push</url>
    <maxConnections>10</maxConnections>
    <batchSize>10000</batchSize>
    <batchTimeoutMs>60000</batchTimeoutMs>
    <encoder class="com.github.loki4j.logback.ProtobufEncoder">
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
        <sortByTime>true</sortByTime>
    </encoder>
</appender>
```
