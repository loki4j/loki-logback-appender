# loki-logback-appender

This project aims to be the fastest and the most lightweight implementation of
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

## Quick start

For Maven project add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.github.log4j</groupId>
  <artifactId>loki-logback-appender</artifactId>
  <version>0.0.1</version>
</dependency>
```

For Gradle project add the following dependency to your `build.gradle`:

```groovy
implementation 'com.github.log4j:loki-logback-appender:0.0.1'
```

Then add Loki appender to your `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.LokiJavaHttpAppender">
    <encoder class="com.github.loki4j.logback.JsonEncoder">
        <label>
            <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
        </label>
        <message>
            <pattern>l=%level h=${HOSTNAME} c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </encoder>
</appender>

<root level="debug">
    <appender-ref ref="LOKI" />
</root>
```

## Key features:

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


## Production readiness

This project is in its early development stage.
Breaking changes (config format, class locations etc.) might be introduced even in minor versions.

That said, we still encourage you to give it a try and provide your feedback to us.
This will help Loki4j evolve and get to a production-ready state eventually.
