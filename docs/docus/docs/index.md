Loki4j is the simplest way to push logs from your Java application
to [Loki](https://grafana.com/oss/loki/) and to connect them with all other metrics
using [Grafana](https://grafana.com/oss/grafana/) dashboards.
No extra tools needed, just add Loki4j appender to your [Logback](http://logback.qos.ch/)
configuration and enjoy.

### Quick Start

For Maven project add the following dependency to your `pom.xml`:

<!--DOCUSAURUS_CODE_TABS-->
<!--Java 11+-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>%version%</version>
</dependency>
```
<!--Java 8-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender-jdk8</artifactId>
    <version>%version%</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.13</version>
</dependency>
```
<!--END_DOCUSAURUS_CODE_TABS-->

For Gradle project add the following dependency to your `build.gradle`:

<!--DOCUSAURUS_CODE_TABS-->
<!--Java 11+-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender:%version%'
```
<!--Java 8-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender-jdk8:%version%'
implementation 'org.apache.httpcomponents:httpclient:4.5.13'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then add Loki appender to your `logback.xml`:

<!--DOCUSAURUS_CODE_TABS-->
<!--Java 11+-->

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
        <sortByTime>true</sortByTime>
    </format>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

<!--Java 8-->

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
        <sortByTime>true</sortByTime>
    </format>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```
<!--END_DOCUSAURUS_CODE_TABS-->

For more details, please refer to [Docs](docs/configuration).

Migrating from the previous version? Read the [Migration Guide](docs/migration).

### Key Features:

- **Support for both JSON and Protobuf formats.**
With Loki4j you can try out both JSON and Protobuf API for sending log records to Loki.
[Learn more...](docs/configuration#switching-to-protobuf-format)

- **Compatibility with Grafana Cloud.**
Loki4j supports HTTP basic authentication, so you can use it for hosted Loki services (e.g. Grafana Cloud)
as well as for on-premise Loki instances.
See the [example](docs/configuration#sending-logs-to-grafana-cloud)...

- **Optionally sort log records before sending to Loki.**
In order to prevent log records loss, Loki4j can sort log records by timestamp inside each batch,
so they will not be rejected by Loki with 'entry out of order' error.

- **Format both labels and messages using Logback patterns.**
Loki4j allows you to use all the power and flexibility of
[Logback patterns](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout)
both for labels and messages.
Same patterns are used in Logback's standard `ConsoleAppender` or `FileAppender`,
so you are probably familiar with the syntax.

- **No JSON library bundled.**
Instead of bundling with any JSON library (e.g. Jackson),
Loki4j comes with a small part of JSON rendering functionality borrowed from [DSL-JSON](https://github.com/ngs-doo/dsl-json/).

- **Zero-dependency.**
Loki4j does not bring any new transitive dependencies to your project,
assuming that you already use `logback-classic` for logging.
See the [example](docs/configuration#minimalistic-zero-dependency-configuration)...

- **Logging performance metrics.**
You can monitor Loki4j's performance (e.g. encode/send duration, number of batches sent, etc.)
by enabling instrumentation powered by [Micrometer](https://micrometer.io/).
[Learn more...](docs/performance)

### Project Status

This project is in active development stage.
In rare cases breaking changes (config format, class locations, etc.) might be introduced in minor versions.
Such cases will be explicitly documented for each release.
Please check [Releases](https://github.com/loki4j/loki-logback-appender/releases) page before you upgrade.

If you are interested in this project, please drop a ☆ on [GitHub](https://github.com/loki4j/loki-logback-appender).