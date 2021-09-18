# loki-logback-appender

![Build](https://img.shields.io/github/workflow/status/loki4j/loki-logback-appender/build/main)
![Maven Central](https://img.shields.io/maven-central/v/com.github.loki4j/loki-logback-appender?color=blue)

Loki4j aims to be the fastest and the most lightweight implementation of
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

Please proceed to the microsite for more information:

- [Quick Start](https://loki4j.github.io/loki-logback-appender/#quick-start)
- [Configuration Guide](https://loki4j.github.io/loki-logback-appender/docs/appenders)

If you have found this project helpful, please drop a :star:!

## Key features

- Support for both JSON and Protobuf formats
- Compatibility with Grafana Cloud
- Optional sorting of log records by timestamp before sending them to Loki
- Logback formatting patterns are used for both labels and messages
- Zero-dependency (for Java 11+)
- Logging performance metrics

More details and links to examples can be found [here](https://loki4j.github.io/loki-logback-appender/#key-features).

## Contributing

Please start with the [Contribution guidelines](CONTRIBUTING.md).

## Building and testing the project

Please make sure the following software is installed on your machine
so you can build and test the project:

- Java 11 or later
- Gradle 7.0 or later

Check out the project to the directory on your local machine and run:

```sh
gradle check
```

## Project status

At the moment all the main logging features are implemented and stabilized.

Further development will be concentrated on bug fixes (if any), keeping up with new versions of Loki, and
improving the codebase so it's easier to maintain.
