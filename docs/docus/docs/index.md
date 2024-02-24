Loki4j is the simplest way to push logs from your Java application
to [Loki](https://grafana.com/oss/loki/) and to connect them with all other metrics
using [Grafana](https://grafana.com/oss/grafana/) dashboards.
No extra tools needed, just add Loki4j appender to your [Logback](http://logback.qos.ch/)
configuration and enjoy.

### Quick Start

The current stable version of Loki4j requires at least Java 11 and Logback v1.3.x.
See the [compatibility matrix](docs/compatibility) for more information about older versions support.

Add the following dependency to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-logback-appender</artifactId>
    <version>%version%</version>
</dependency>
```
<!--Gradle-->

```groovy
implementation 'com.github.loki4j:loki-logback-appender:%version%'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then add Loki appender to your `logback.xml`:

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

For more details, please refer to [Docs](docs/configuration).

Migrating from the previous Loki4j version? Read the [Migration Guide](docs/migration).

### Key Features:

- **Flexible management of Loki labels using MDC and SLF4J Markers.**
You can specify Loki labels dynamically for any set of log records, and even on per-record basis.
[Learn more...](docs/labels)

- **Out of the box JSON layout support for log message formatting.**
You choose between plain text and JSON (Logstash-like) log message formatting.
[Learn more...](docs/jsonlayout)

- **Logback plain text formatting patterns can be used for both labels and messages.**
Loki4j allows you to use all the power and flexibility of
[Logback patterns](http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout)
both for labels and messages.
Same patterns are used in Logback's standard `ConsoleAppender` or `FileAppender`,
so you are probably familiar with the syntax.

- **Support for JSON and Protobuf Loki API flavours.**
With Loki4j you can try out either JSON or Protobuf API for sending log records to Loki.
[Learn more...](docs/protobuf)

- **Compatibility with Grafana Cloud.**
Loki4j supports HTTP basic authentication, so you can use it for hosted Loki services (e.g. Grafana Cloud)
as well as for on-premise Loki instances.
See the [example](docs/grafanacloud)...

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

At the moment all the main logging features are implemented and stabilized.

Further development will be concentrated on bug fixes (if any), keeping up with new versions of Loki, and
improving the codebase so it's easier to maintain.

If you have found this project helpful, please drop a ☆ on [GitHub](https://github.com/loki4j/loki-logback-appender).
