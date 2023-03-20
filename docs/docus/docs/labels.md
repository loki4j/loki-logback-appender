---
id: labels
title: Managing Loki labels
sidebar_label: Managing Loki Labels
---

## Organizing labels

Logs in Loki are indexed using labels that have key-value format.
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

Below we will go through some tips and tricks you can use in `format.label` section to make your life a bit easier.

By default labels are defined as `key=value` pairs separated by comma.

```xml
<label>
    <pattern>app=my-app,host=${HOSTNAME},level=%level</pattern>
</label>
```

But you can override `pairSeparator` to organize them in a different way.
For example, if you have many labels, it's better to have each of them on a separate line:

```xml
<label>
    <pattern>
        job=loki4j
        app=my-app
        // you even can write comments here
        namespace_name=${NAMESCAPE_NAME}
        pod_name=${POD_NAME}
        level=%level
    </pattern>
    <pairSeparator>regex:(\n|//[^\n]+)+</pairSeparator>
</label>
```

Please note, that in the example above the regular expression in `pairSeparator` defines lines starting with `//` a part of a separator.
So now we have a `// comment` feature here as well.

## Using MDC in labels

`label.pattern` is nothing but Logback's [pattern layout](https://logback.qos.ch/manual/layouts.html#ClassicPatternLayout), which means it supports [MDC](https://logback.qos.ch/manual/mdc.html):

```xml
<label>
    <pattern>app=my-app,level=%level,stage=%mdc{stage:-none}</pattern>
</label>
```

## Adding dynamic labels using Markers

In classic Logback, markers are typically used to [filter](https://logback.qos.ch/manual/filters.html#TurboFilter) log records.
In Loki4j you can also use markers to dynamically set Loki labels for any particular log message.

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