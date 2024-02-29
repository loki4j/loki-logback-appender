---
id: compatibility
title: Loki4j Compatibility Matrix
sidebar_label: Compatibility Matrix
---

The versions of Loki4j that had introduced backward-incompatible platform upgrades are listed in the table below.

|Loki4j|Java|Logback|
|-------|-------|-----------|
|v1.5.0||v1.3.x|
|v0.3.0|[Java 8](#java-8-support), Java 11+||
|v0.1.0|Java 11+|v1.2.x|


### Java 8 support

Loki4j ships a separate artifact with the `-jdk8` suffix built specially for Java 8.
Furthermore, we would need to use the Apache HTTP client with Java 8, as the default Java HTTP client works only for Java 11+.
So please add the following dependencies to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender-jdk8</artifactId>
    <version>1.5.0</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>
```

<!--Gradle-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender-jdk8:1.5.0'
implementation 'org.apache.httpcomponents:httpclient:4.5.14'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you need to explicitly specify `ApacheHttpSender` by setting `class` attribute for `http` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        ...
    </http>
</appender>
```

A minimalistic appender configuration compatible with Java 8 might look like this:

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