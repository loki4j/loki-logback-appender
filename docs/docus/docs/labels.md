---
id: labels
title: Managing Loki labels
sidebar_label: Managing Loki Labels
---

## Organizing labels

Logs in Loki are indexed using labels that have a key-value format.
In Loki4j you can specify labels on per-record level using all the benefits Logback has to offer.

Labels are set in `format.label` section of `Loki4jAppender`'s config:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format>
        <label>
            <-- All label settings go here -->
        </label>
        ...
    </format>
    ...
</appender>
```

Below, we will go through some tips and tricks you can use in the `format.label` section to make your life a bit easier.

By default labels are defined as `key=value` pairs separated by commas.

```xml
<label>
    <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
</label>
```

If you have many labels, you can put them in multiple lines (a trailing comma is optional):

```xml
<label>
    <pattern>
        job = loki4j,
        app = my-app,
        namespace_name = ${NAMESCAPE_NAME},
        pod_name = ${POD_NAME},
        level=%level,
    </pattern>
</label>
```

## Using MDC in labels

`label.pattern` is nothing but Logback's [pattern layout](https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout), which means it supports [MDC](https://logback.qos.ch/manual/mdc.html):

```xml
<label>
    <pattern>app=my-app,level=%level,stage=%mdc{stage:-none}</pattern>
</label>
```

## Adding dynamic labels using Markers

In classic Logback, markers are typically used to [filter](https://logback.qos.ch/manual/filters.html#TurboFilter) log records.
With Loki4j you can also use markers to set Loki labels dynamically for any particular log message.

First, you need to make Loki4j scan markers attached to each log event by enabling `readMarkers` flag:

```xml
<label>
    <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
    <readMarkers>true</readMarkers>
</label>
```

Then you can set one or more key-value labels to any specific log line using `LabelMarker`:

```java
import com.github.loki4j.slf4j.marker.LabelMarker;

...

void handleException(Exception ex) {
    var marker = LabelMarker.of("exceptionClass", () -> ex.getClass().getSimpleName());
    log.error(marker, "Unexpected error", ex);
}
```

## Best practices

We encourage you to follow the [Label best practices](https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/) collected by Grafana Loki team. Loki4j provides several settings to facilitate these recommendations.

First, make sure you have `format.staticLabels` flag enabled.
This will prevent Loki4j from calculating labels for each particular log record:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <format>
        <staticLabels>true</staticLabels>
        ...
     </format>
</appender>
```

Second, make sure you put all the high-cardinality metadata to [structured metadata](metadata.md) instead of labels.
