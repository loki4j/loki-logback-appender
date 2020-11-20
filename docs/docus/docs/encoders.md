---
id: encoders
title: Encoders
sidebar_label: Encoders
---

## Choosing the right encoder

Loki4j provides two message encoders for Loki:

- `JsonEncoder`, converts log batches into JSON format specified by Loki API
- `ProtobufEncoder`, converts log batches into Protobuf format specified by Loki API
(check the details in [dedicated setion](#protobufencoder))

There are some use-case specific recommendation for choosing one or another appender:

1. If your project generates large amount of logs, prefer `ProtobufEncoder`
2. If your project already depends Google Protobuf, prefer `ProtobufEncoder`

In other cases you can use whatever appender works for you.
If you are still not sure, use `JsonEncoder`.

Set the encoder of your choice for Loki appender in your `logback.xml` configuration:

<!--DOCUSAURUS_CODE_TABS-->
<!--JsonEncoder-->

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- define appender settings here -->
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <!-- define encoder settings here -->
    </encoder>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

<!--ProtobufEncoder-->

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <!-- define appender settings here -->
    <encoder class="com.github.loki4j.logback.ProtobufEncoder">
        <!-- define encoder settings here -->
    </encoder>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

<!--END_DOCUSAURUS_CODE_TABS-->

We are going to describe available encoder settings in the next sections.

## Label settings

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
    <!-- If you use only one label for all log records, you can -->
    <!-- set this flag to true and save some CPU time on grouping records by label -->
    <staticLabels>false</staticLabels>
</encoder>
```

## Message settings

```xml
<encoder class="com.github.loki4j.logback.JsonEncoder">
    <message>
        <!-- Logback pattern to use for log record's message -->
        <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
    </message>
    <!-- If true, log records in batch are sorted by timestamp -->
    <!-- If false, records will be sent to Loki in arrival order -->
    <!-- Enable this if you see 'entry out of order' error from Loki -->
    <sortByTime>false</sortByTime>
</encoder>
```

## ProtobufEncoder

`ProtobufEncoder` requires you to add Protobuf-related dependencies to your project.

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
