---
id: metadata
title: Structured metadata
sidebar_label: Structured Metadata
---

[Structured metadata](https://grafana.com/docs/loki/latest/get-started/labels/structured-metadata/) is a way to attach high-cardinality metadata to logs without indexing them or including them in the log line content itself.
Structured metadata is a set of key-value pairs; both keys and values must be plain strings.
This feature was introduced in Loki v2.9.0.

In Loki4j you can configure structured metadata as a Logback pattern similar to [labels](labels.md):

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

Settings from label section, such as `pairSeparator`, `keyValueSeparator`, and `readMarkers` apply to both labels and structure metadata.

Similarly to labels, if `readMarkers` flag is enabled, you can attach structured metadata to a particular log message dynamically from your Java code using SLF4J marker:

```java
import com.github.loki4j.slf4j.marker.StructuredMetadataMarker;

...

void handleException(Exception ex) {
    var marker = StructuredMetadataMarker.of("exceptionClass", () -> ex.getClass().getSimpleName());
    log.error(marker, "Unexpected error", ex);
}
```
