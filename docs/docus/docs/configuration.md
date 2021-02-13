---
id: configuration
title: Loki4j Configuration
sidebar_label: Configuration
---

## Reference

Most Loki4j appender settings are optional. These few that are required are marked explicitly.

### General settings

Setting|Default|Description
-------|-------|-----------
batchSize|1000|Max number of messages to put into single batch and send to Loki
batchTimeoutMs|60000|Max time in milliseconds to wait before sending a batch to Loki
processingThreads|1|Number of threads to use for log message processing and formatting
verbose|false|If true, the appender will print its own debug logs to stderr

### HTTP settings

Setting|Default|Description
-------|-------|-----------
http.url||**Required**. Loki endpoint to be used for sending batches
http.connectionTimeoutMs|30000|Time in milliseconds to wait for HTTP connection to Loki to be established before reporting an error
http.requestTimeoutMs|5000|Time in milliseconds to wait for HTTP request to Loki to be responded before reporting an error
http.auth.username||Username to use for basic auth
http.auth.password||Password to use for basic auth

### Format settings

Setting|Default|Description
-------|-------|-----------
format.label.pattern||**Required**. Logback pattern to use for log record's label
format.label.pairSeparator|,|Character to use as a separator between labels
format.label.keyValueSeparator|=|Character to use as a separator between label's name and its value
format.label.nopex|true|If true, exception info is not added to labels. If false, you should take care of proper formatting
format.message.pattern||**Required**. Logback pattern to use for log record's message
format.staticLabels|false|If you use only one label for all log records, you can set this flag to true and save some CPU time on grouping records by label
format.sortByTime|false|If true, log records in batch are sorted by timestamp. If false, records will be sent to Loki in arrival order. Enable this if you see 'entry out of order' error from Loki

### Using Apache HttpClient

By default Loki4j uses `JavaHttpSender`, backed by `java.net.http.HttpClient` available in Java 11 and later.
This sender does not require any extra dependencies.

However, you may want to switch to `ApacheHttpSender`, backed by `org.apache.http.client.HttpClient` available for Java 8+ projects.
In this case you need to ensure you have added the required dependencies to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.13</version>
</dependency>
```

<!--Gradle-->

```groovy
compile 'org.apache.httpcomponents:httpclient:4.5.13'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you can explicitly specify `ApacheHttpSender` by setting `class` attribute for `http` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        ...
    </http>
</appender>
```

`ApacheHttpSender` shares most of the settings with `JavaHttpSender`.
However, there are some specific settings available only for `ApacheHttpSender`:

Setting|Default|Description
-------|-------|-----------
http.maxConnections|1|Maximum number of HTTP connections to keep in the pool
http.connectionKeepAliveMs|120000|A duration of time in milliseconds which the connection can be safely kept idle for later reuse. This value should not be greater than `server.http-idle-timeout` in your Loki config
http.httpThreads|1|Number of threads to use for sending HTTP requests

### Switching to Protobuf format

By default Loki4j uses `JsonEncoder` that converts log batches into JSON format specified by Loki API.
This encoder does not use any extra libs for JSON generation.

If you want to use `ProtobufEncoder`, you need to add Protobuf-related dependencies to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.14.0</version>
</dependency>
<dependency>
    <groupId>org.xerial.snappy</groupId>
    <artifactId>snappy-java</artifactId>
    <version>1.1.8</version>
</dependency>
```

<!--Gradle-->

```groovy
compile 'com.google.protobuf:protobuf-java:3.12.4'
compile 'org.xerial.snappy:snappy-java:1.1.8'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you can explicitly specify `ProtobufEncoder` by setting `class` attribute for `format` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format class="com.github.loki4j.logback.ProtobufEncoder">
        ...
    </format>
</appender>
```

## Examples

### Overriding the default settings

In this example we would like to change max batch size to 100 records, batch timeout to 10s, label key-value separator to `:`,
and sort log records by time before sending them to Loki.
Also we would like to use [Apache HTTP sender](#using-apache-httpclient) with a pool of 10 connections and [Protobuf API](#switching-to-protobuf-format).
Finally, we want to see Loki4j debug output.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <batchSize>100</batchSize>
    <batchTimeoutMs>10000</batchTimeoutMs>
    <verbose>true</verbose>
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        <url>http://localhost:3100/loki/api/v1/push</url>
        <maxConnections>10</maxConnections>
    </http>
    <format class="com.github.loki4j.logback.ProtobufEncoder">
        <label>
            <pattern>app:my-app,host:${HOSTNAME}</pattern>
            <keyValueSeparator>:</keyValueSeparator>
        </label>
        <message>
            <pattern>l=%level c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
        <sortByTime>true</sortByTime>
    </format>
</appender>
```

### Minimalistic zero-dependency configuration

In Java 11 and later we can use standard HTTP client and Loki JSON API.
This setup is supported natively by Loki4j and does not require any extra dependencies.

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

### Minimalistic configuration compatible with Java 8

In this example we would like to define only required settings.
We would have to use Apache HTTP sender because the default Java HTTP sender works only for Java 11+.
Check the corresponding [configuration section](#using-apache-httpclient) for details.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http class="com.github.loki4j.logback.ApacheHttpSender">
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

### Sending logs to Grafana Cloud

In this example we will see how to send log records to hosted Loki service (e.g. Grafana Cloud).
We will need to specify the credentials and increase the request timeout to 15s.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>https://logs-prod-us-central1.grafana.net/loki/api/v1/push</url>
        <auth>
            <username>example_username</username>
            <password>example_api_token</password>
        </auth>
        <requestTimeoutMs>15000</requestTimeoutMs>
    </http>
    <format>
        <label>
            <pattern>app=my-app</pattern>
        </label>
        <message>
            <pattern>l=%level c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </format>
</appender>
```
