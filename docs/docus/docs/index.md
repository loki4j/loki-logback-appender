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

### Key Features:

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

### Project Status

This project is in active development stage.
In rare cases breaking changes (config format, class locations, etc.) might be introduced in minor versions.
Such cases will be explicitly documented for each release.
Please check [Releases](https://github.com/loki4j/loki-logback-appender/releases) page before you upgrade.

If you are interested in this project, please drop a ☆ on [GitHub](https://github.com/loki4j/loki-logback-appender).