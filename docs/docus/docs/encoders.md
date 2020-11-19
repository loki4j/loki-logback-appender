---
id: encoders
title: Encoders
sidebar_label: Encoders
---

## JsonEncoder

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

## ProtobufEncoder

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
