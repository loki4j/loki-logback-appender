# loki-logback-appender

![Build](https://img.shields.io/github/actions/workflow/status/loki4j/loki-logback-appender/build-and-test.yaml?branch=main)
![Maven Central](https://img.shields.io/maven-central/v/com.github.loki4j/loki-logback-appender?color=blue)

Loki4j aims to be the fastest and the most lightweight implementation of
[Logback](http://logback.qos.ch/) appender for [Loki](https://grafana.com/oss/loki/).
This project is unofficial and community-driven.

Please proceed to the microsite for more information:

- [Quick Start](https://loki4j.github.io/loki-logback-appender/#quick-start)
- [Configuration Guide](https://loki4j.github.io/loki-logback-appender/docs/configuration)

If you have found this project helpful, please drop a :star:!

## Key features

- Flexible management of Loki labels using MDC and SLF4J Markers
- Out-of-the-box JSON layout support for log message formatting
- Logback plain text formatting patterns can be used for both labels and messages
- Support for JSON and Protobuf Loki API flavors
- Compatibility with Grafana Cloud
- Zero-dependency
- Logging performance metrics

More details and links to examples can be found [here](https://loki4j.github.io/loki-logback-appender/#key-features).

## Contributing

Please start with the [Contribution guidelines](CONTRIBUTING.md).

## Building and testing the project

Please make sure the following software is installed on your machine
so you can build and test the project:

- Java 11 or later

Check out the project in the directory on your local machine and run:

```sh
./gradlew check
```

## Project status

At the moment all the main logging features have been implemented and stabilized.

Further development will be concentrated on bug fixes (if any), keeping up with new versions of Loki, and
improving the codebase so it's easier to maintain.
