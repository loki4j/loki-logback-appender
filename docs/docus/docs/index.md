Loki4j aims to be the fastest and the most lightweight implementation of 
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

### Quick Start

For Maven project add the following dependency to your `pom.xml`:

<!--DOCUSAURUS_CODE_TABS-->
<!--Java 11+-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>0.3.1</version>
</dependency>
```
<!--Java 8-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender-jdk8</artifactId>
    <version>0.3.1</version>
</dependency>
```
<!--END_DOCUSAURUS_CODE_TABS-->

For Gradle project add the following dependency to your `build.gradle`:

<!--DOCUSAURUS_CODE_TABS-->
<!--Java 11+-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender:0.3.1'
```
<!--Java 8-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender-jdk8:0.3.1'
```
<!--END_DOCUSAURUS_CODE_TABS-->

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

For more details, please refer to [Docs](docs/appenders).

### Project Status

This project is in active development stage.
In rare cases breaking changes (config format, class locations, etc.) might be introduced in minor versions.
Such cases will be explicitly documented for each release.
Please check [Releases](https://github.com/loki4j/loki-logback-appender/releases) page before you upgrade.
