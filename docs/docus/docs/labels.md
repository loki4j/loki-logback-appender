---
id: labels
title: Managing Loki labels and structured metadata
sidebar_label: Labels and structured metadata
---

## Labels vs Structured metadata

*Labels* are used by Loki for indexing log records, and thus, improving the log search performance.
Labels have a key-value format; both keys and values must be plain strings.
Loki4j allows you to use any [Logback pattern](https://logback.qos.ch/manual/layouts.html#conversionWord) as a label value.

Labels are defined as `key=value` pairs separated by commas in `format.label` section of `Loki4jAppender`'s config.
If you have many labels, you can put them in multiple lines (a trailing comma is optional):

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format>
        <label>
            <pattern>
                app=my-app,
                host=${HOSTNAME},
                level=%level
            </pattern>
        </label>
        ...
    </format>
    ...
</appender>
```

However, labels have a significant limitation: you can use them only for low-cardinality values.

*Structured metadata* is a way to attach high-cardinality metadata to logs without indexing them or including them in the log line itself.
This feature was introduced in Loki v2.9.0.
It overcomes limitations of labels, still increasing search efficiency as Loki does not have to scan entire message bodies for metadata.
For further details, please check Loki's [docs](https://grafana.com/docs/loki/latest/get-started/labels/structured-metadata/).

Similarly to labels, structured metadata is a set of key-value pairs.
You can configure it using Logback patterns as well.

It is recommended to use both labels and structured metadata in your configuration.
Labels should be a small set of static low-cardinality values.
Any other metadata you want to attach should go to structured metadata:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <format>
        <label>
            <!-- Logback pattern for labels -->
            <pattern>
                app = my-app,
                host = ${HOSTNAME}
            </pattern>
            <!-- Logback pattern for structured metadata -->
            <structuredMetadataPattern>
                level = %level,
                thread = %thread,
                class = %logger,
                traceId = %mdc{traceId:-none}
            </structuredMetadataPattern>
        </label>
        <staticLabels>true</staticLabels>
        ...
    </format>
</appender>
```

`label.pattern` and `structuredMetadataPattern` is nothing but Logback's [pattern layout](https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout).
No wonder you can use [MDC](https://logback.qos.ch/manual/mdc.html) as in the example above.


## Adding dynamic labels using Markers

In classic Logback, markers are typically used to [filter](https://logback.qos.ch/manual/filters.html#TurboFilter) log records.
With Loki4j you can also use markers to set Loki labels (or structured metadata) dynamically for any particular log message.

First, you need to make Loki4j scan markers attached to each log event by enabling `readMarkers` flag:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format>
        <label>
            ...
            <readMarkers>true</readMarkers>
        </label>
        ...
    </format>
    ...
</appender>
```

Then you can set one or more key-value pairs to any specific log record's metadata using `StructuredMetadataMarker`:

```java
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;

...

void handleException(Exception ex) {
    var marker = StructuredMetadataMarker.of("exceptionClass", () -> ex.getClass().getSimpleName());
    log.error(marker, "Unexpected error", ex);
}
```

Although it's not recommended (better keep you labels static), you can use `LabelMarker` to add some labels as well:

```java
import com.github.loki4j.slf4j.marker.LabelMarker;

...

void handleException(Exception ex) {
    var marker = LabelMarker.of("exceptionClass", () -> ex.getClass().getSimpleName());
    log.error(marker, "Unexpected error", ex);
}
```
