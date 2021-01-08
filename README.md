# loki-logback-appender

![Build](https://img.shields.io/github/workflow/status/loki4j/loki-logback-appender/build/main)
![Maven Central](https://img.shields.io/maven-central/v/com.github.loki4j/loki-logback-appender?color=blue)

Loki4j aims to be the fastest and the most lightweight implementation of
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

Please proceed to the microsite for more information:

- [Quick Start](https://loki4j.github.io/loki-logback-appender/#quick-start)
- [Configuration Guide](https://loki4j.github.io/loki-logback-appender/docs/appenders)

If you are interested in this project, please drop a :star:!

## Key features

- Support for both JSON and Protobuf formats
- Compatibility with Grafana Cloud
- Optionally order log records before sending to Loki
- Use Logback patterns for labels and messages formatting
- Zero-dependency (for Java 11+)
- Logging performance metrics

## Contributing

Please start with the [Contribution guidelines](CONTRIBUTING.md).

## Building and testing the project

Please make sure the following software is installed on you machine
so you can build and test the project:

- Java 11 or later
- Gradle 6.7 or later

Check out the project to the directory on you local machine and run:

```sh
gradle check
```

## Production readiness

This project is in active development stage.
In rare cases breaking changes (config format, class locations, etc.) might be introduced in minor versions.
Such cases will be explicitly documented for each release.
Please check [Releases](https://github.com/loki4j/loki-logback-appender/releases) page before you upgrade.
