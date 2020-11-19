---
id: appenders
title: Appenders
sidebar_label: Appenders
---

## LokiJavaHttpAppender

`LokiJavaHttpAppender` is backed by `java.net.http.HttpClient` available in Java 11 and later.
Thus, `LokiJavaHttpAppender` does not require you to add any additional dependencies to you project.

Below is the complete `LokiJavaHttpAppender` configuration reference with default values:

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
    <!-- Max number of messages to put into single batch and send to Loki -->
    <batchSize>1000</batchSize>
    <!-- Max time in milliseconds to wait before sending a batch to Loki -->
    <batchTimeoutMs>60000</batchTimeoutMs>
    <!-- Number of threads to use for log message processing and formatting -->
    <processingThreads>1</processingThreads>
    <!-- Number of threads to use for sending HTTP requests -->
    <httpThreads>1</httpThreads>
    <!-- If true, appender will pring its own debug logs to stderr -->
    <verbose>false</verbose>
    <!-- An encoder to use for converting log record batches to format acceptable by Loki -->
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <!-- See encoder-specific settings reference in the section dedicated to the particular encoder -->
    </encoder>
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

`LokiApacheHttpAppender` shares most of the settings with `LokiJavaHttpAppender`,
please refer [here](#lokijavahttpappender) for details.
However, there are some client-specific settings with their default values:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiApacheHttpAppender">
    ...
    <!-- Max number of HTTP connections setting for HttpClient -->
    <maxConnections>100</maxConnections>
    <!-- Keep-alive setting for HttpClient -->
    <keepAlive>true</keepAlive>
</appender>
```
