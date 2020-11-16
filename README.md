# loki-logback-appender

![Build](https://img.shields.io/github/workflow/status/loki4j/loki-logback-appender/build/main)
![Maven Central](https://img.shields.io/maven-central/v/com.github.loki4j/loki-logback-appender?color=blue)

Loki4j aims to be the fastest and the most lightweight implementation of
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

## Quick start

If your project is on Java 8, please check [Java 8 support](#java-8-support) section.

If your project is on Java 11 or later, please follow the instructions below.

For Maven project add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>0.3.1</version>
</dependency>
```

For Gradle project add the following dependency to your `build.gradle`:

```groovy
implementation 'com.github.loki4j:loki-logback-appender:0.3.1'
```

Then add Loki appender to your `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <url>http://localhost:3100/loki/api/v1/push</url>
    <batchSize>100</batchSize>
    <batchTimeoutMs>10000</batchTimeoutMs>
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
        <sortByTime>true</sortByTime>
    </encoder>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

For more details, please refer to [Configuration](#configuration) section.

## Key features:

- **Support for both JSON and Protobuf formats.**
With Loki4j you can try out both JSON and Protobuf API for sending log records to Loki.

- **Optionally order log records before sending to Loki.**
In order to prevent log records loss, Loki4j can sort log records by timestamp inside each batch,
so they will not be rejected by Loki with 'entry out of order' error.

- **Use Logback patterns for labels and messages formatting.**
Loki4j allows you to use all the power and flexibility of
[Logback patterns](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout)
both for labels and messages.
Same patterns are used in Logback's standard `ConsoleAppender` or `FileAppender`,
so you are probably familiar with the syntax.

- **No JSON library bundled.**
Instead of bundling with any JSON library (e.g. Jackson),
Loki4j comes with a small part of JSON rendering functionality it needs embedded.

- **Zero-dependency.**
Loki4j does not bring any new transitive dependencies to your project,
assuming that you already use `logback-classic` for logging.


## Configuration

### LokiJavaHttpAppender

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

### LokiApacheHttpAppender

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

### JsonEncoder

`JsonEncoder` converts log batches into JSON format specified by Loki API.
This encoder does not require you to add any additional dependencies to you project.

Below is the complete `JsonEncoder` configuration reference with default values:

```xml
<encoder class="com.github.loki4j.logback.JsonEncoder">
    <label>
        <!-- Logback pattern to use for log record's label -->
        <pattern>host=${HOSTNAME},level=%level</pattern>
        <!-- Character to use as a separator between labels -->
        <pairSeparator>,</pairSeparator>
        <!-- Character to use as a separator between label's name and its value -->
        <keyValueSeparator>=</keyValueSeparator>
        <!-- If true, exception info is not added to labels -->
        <!-- If false, you should take care of proper formatting -->
        <nopex>true</nopex>
    </label>
    <message>
        <!-- Logback pattern to use for log record's message -->
        <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
    </message>
    <!-- If true, log records in batch are sorted by timestamp -->
    <!-- If false, records will be sent to Loki in arrival order -->
    <!-- Enable this if you see 'entry out of order' error from Loki -->
    <sortByTime>false</sortByTime>
    <!-- If you use only one label for all log records, you can -->
    <!-- set this flag to true and save some CPU time on grouping records by label -->
    <staticLabels>false</staticLabels>
</encoder>
```

### ProtobufEncoder

`ProtobufEncoder` converts log batches into Protobuf format specified by Loki API.
This encoder requires you to add Protobuf-related dependencies to your project.

Maven:

```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.12.4</version>
</dependency>
<dependency>
    <groupId>org.xerial.snappy</groupId>
    <artifactId>snappy-java</artifactId>
    <version>1.1.8</version>
</dependency>
```

Gradle:

```groovy
compile 'com.google.protobuf:protobuf-java:3.12.4'
compile 'org.xerial.snappy:snappy-java:1.1.8'
```

Example configuration using `ProtobufEncoder`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <url>http://localhost:3100/loki/api/v1/push</url>
    <batchSize>100</batchSize>
    <batchTimeoutMs>10000</batchTimeoutMs>
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

For complete list of properties available, please refer to [JsonEncoder](#jsonencoder)
as these two encoders have the same set of properties at the moment.

## Java 8 support

Loki4j provides an experimental support for Java 8.
Check the Quick Start instruction below.

For Maven project add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender-jdk8</artifactId>
    <version>0.3.1</version>
</dependency>
```

For Gradle project add the following dependency to your `build.gradle`:

```groovy
implementation 'com.github.loki4j:loki-logback-appender-jdk8:0.3.1'
```

Please note that only Apache HttpClient-based appender is currently available in Java 8 version.
Check the corresponding [Configuration section](#lokiapachehttpappender) for details.

## Contributing

Please start with the [Contribution guidelines](CONTRIBUTING.md).

## Production readiness

This project is in active development stage.
In rare cases breaking changes (config format, class locations, etc.) might be introduced in minor versions.
Such cases will be explicitly documented for each release.
Please check [Releases](releases) page before the upgrade.
