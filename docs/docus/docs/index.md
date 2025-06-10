Loki4j is the simplest way to push logs from your Java application
to [Loki](https://grafana.com/oss/loki/) and to connect them with all other metrics
using [Grafana](https://grafana.com/oss/grafana/) dashboards.
No extra tools needed, just add Loki4j appender to your [Logback](http://logback.qos.ch/)
configuration and enjoy.

### Quick Start

The current stable version of Loki4j requires **Java 11+** and **Logback v1.5.x**.
See the [compatibility matrix](docs/compatibility) for more information about older versions' support.

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
<contextName>my-app</contextName>

<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>http://localhost:3100/loki/api/v1/push</url>
    </http>
</appender>

<root level="DEBUG">
    <appender-ref ref="LOKI" />
</root>
```

For more details, please refer to [Docs](docs/configuration).

Migrating from the previous Loki4j version? Read the [Migration Guide](docs/migration).

### Key Features:

- **Dynamic generation of Loki labels and metadata out of any Logback pattern, MDC, KVP, or SLF4J markers.**
Label values are specified as Logback patterns.
This along with MDC, KVP, and markers allows you to precisely control label set for each particular log record.
[Learn more...](docs/labels)

- **Structured metadata support.**
Unlike labels, structured metadata is not indexed, but it still can significantly increase search efficiency,
as Loki does not have to scan entire message bodies for metadata.
Loki4j provides the same capabilities of dynamic generation for structured metadata as it does for labels.
[Learn more...](docs/labels)

- **Fast JSON layout for log message formatting.**
If you prefer Logstash-like log message formatting, you can switch the layout from plain text to JSON.
[Learn more...](docs/jsonlayout)

- **Support of JSON and Protobuf Loki API flavors.**
By default, JSON endpoints are used, but you can switch to Protobuf API anytime.
[Learn more...](docs/protobuf)

- **Compatibility with Grafana Cloud.**
Loki4j supports HTTP basic authentication, so you can use it for hosted Loki services (e.g., Grafana Cloud)
as well as for on-premise Loki instances.
See the [example](docs/grafanacloud)...

- **Zero-dependency.**
Loki4j jar has a very small footprint.
It does not bring any new transitive dependencies to your project, assuming you already use `logback-classic` for logging.
See the [example](docs/configuration#minimalistic-zero-dependency-configuration)...

- **Performance metrics.**
You can monitor Loki4j's performance (e.g., encode/send duration, number of batches sent, etc.)
by enabling instrumentation powered by [Micrometer](https://micrometer.io/).
[Learn more...](docs/performance)

### Project Status

At the moment all the main logging features have been implemented and stabilized.

Further development will be concentrated on bug fixes (if any), keeping up with new versions of Loki, and
improving the codebase so it's easier to maintain.

If you have found this project helpful, please drop a ☆ on [GitHub](https://github.com/loki4j/loki-logback-appender).
