---
id: appenders
title: Appenders
sidebar_label: Appenders
---

## Choosing the right appender

Loki4j provides two HTTP appenders for Loki:

- `LokiJavaHttpAppender`, backed by `java.net.http.HttpClient` available in Java 11 and later
- `LokiApacheHttpAppender`, backed by `org.apache.http.client.HttpClient`
(check the details in [dedicated section](#lokiapachehttpappender))

There are some use-case specific recommendation for choosing one or another appender:

1. If your project is on Java 8, you can only use `LokiApacheHttpAppender`
2. If your project doesn't depend on Apache HttpClient and you don't want to introduce
new dependencies, use `LokiJavaHttpAppender`

In other cases you can use whatever appender works for you.
If you are still not sure, use `LokiJavaHttpAppender`.

Register the appender of your choice in your `logback.xml` configuration:

<!--DOCUSAURUS_CODE_TABS-->
<!--LokiJavaHttpAppender-->

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- define appender settings here -->
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

<!--LokiApacheHttpAppender-->

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiApacheHttpAppender">
    <!-- define appender settings here -->
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

<!--END_DOCUSAURUS_CODE_TABS-->

We are going to describe available appender settings in the next sections.

## Connection settings

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- Loki endpoint to be used for sending batches -->
    <url>http://localhost:3100/loki/api/v1/push</url>
    <!-- Time in milliseconds to wait for HTTP connection to Loki to be established -->
    <!-- before reporting an error -->
    <connectionTimeoutMs>30000</connectionTimeoutMs>
    <!-- Time in milliseconds to wait for HTTP request to Loki to be responded -->
    <!-- before reporting an error -->
    <requestTimeoutMs>5000</requestTimeoutMs>
</appender>
```

## Message batching settings

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- Max number of messages to put into single batch and send to Loki -->
    <batchSize>1000</batchSize>
    <!-- Max time in milliseconds to wait before sending a batch to Loki -->
    <batchTimeoutMs>60000</batchTimeoutMs>
    <!-- Number of threads to use for log message processing and formatting -->
    <processingThreads>1</processingThreads>
    <!-- Number of threads to use for sending HTTP requests -->
    <httpThreads>1</httpThreads>
</appender>
```

## Tracing settings

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- If true, appender will print its own debug logs to stderr -->
    <verbose>false</verbose>
</appender>
```

## LokiApacheHttpAppender

`LokiApacheHttpAppender` is backed by `org.apache.http.client.HttpClient`.
You can use this appender for Java 8+ projects.
You have to add the following dependency to your project in order to use this appender:

Maven:

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.13</version>
</dependency>
```

Gradle:

```groovy
compile 'org.apache.httpcomponents:httpclient:4.5.13'
```

`LokiApacheHttpAppender` shares most of the settings with `LokiJavaHttpAppender`.
However, there are some client-specific settings with their default values:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiApacheHttpAppender">
    ...
    <!-- Maximum number of HTTP connections setting for HttpClient -->
    <maxConnections>1</maxConnections>
    <!-- A duration of time which the connection can be safely kept idle for later reuse. -->
    <!-- This value can not be greater than `server.http-idle-timeout` in your Loki config -->
    <connectionKeepAliveMs>120000</connectionKeepAliveMs>
</appender>
```
