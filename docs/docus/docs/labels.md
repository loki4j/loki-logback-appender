---
id: labels
title: Managing Loki labels and structured metadata
sidebar_label: Labels and structured metadata
---

## Labels vs Structured metadata

*Labels* are used by Loki for indexing log records, and thus, improving the log search performance.
Labels have a key-value format; both keys and values must be plain strings.
Loki4j allows you to use any [Logback pattern](https://logback.qos.ch/manual/layouts.html#conversionWord) as a label value.

Labels are defined one per line as `key=value` pairs in `labels` section of `Loki4jAppender`'s config:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <labels>
        app = my-app
        host = ${HOSTNAME}
        level = %level
    </labels>
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
    <labels>
        app = my-app
        host = ${HOSTNAME}
    </labels>
    <structuredMetadata>
        level = %level
        thread = %thread
        class = %logger
    </structuredMetadata>
    <batch>
        <staticLabels>true</staticLabels>
    </batch>
</appender>
```

## Working with MDC and KVP

What if you want to include entries from Logback's [MDC](https://logback.qos.ch/manual/mdc.html) or [KVP](https://www.slf4j.org/manual.html#fluent) into the labels or structured metadata sent to Loki?

Loki4j supports a special syntax for this called "bulk patterns".
Check the examples below:

- *\* = %%kvp* - include all KVP entries
- *kvp_\* = %%kvp* - include all KVP entries, add prefix "kvp_" for all keys (e.g., "kvp_key1", "kvp_key2", etc.)
- *\* = %%mdc{key1, key2}* - include MDC entries "key1" and "key2"
- *\* != %%kvp{key1, key2}* - include all KVP entries except "key1" and "key2"

Left side (before "=") starts with an optional alphanumeric prefix followed by a wildcard "*".
This wildcard is mandatory, it means the bulk pattern can produce zero or more key-value pairs depending on the right side function.
If prefix is specified, it will be added to each key produced by this bulk pattern.

Right side (after "=") starts with a bulk pattern marker "%%" followed by a function name and an optional param list.
Currently, two functions are supported: "mdc" and "kvp".
Both optionally take a list of keys to include as params.
If no params specified, all MDC/KVP entries are included.

If you want to exclude certain keys, use "!=" instead of "=".

<details>

<summary>Bulk patterns' grammar *(click to expand...)*</summary>

The generic form of a bulk pattern can be represented as the following grammar:

```bnf
bulk-pattern : [<prefix>]"*" ["!"]"=" "%%"<func-name>["{"<key-list>"}"]
prefix       : <id>
func-name    : "mdc" | "kvp"
key-list     : <key>[, <key-list>]
key          : <id>
```
Square brackets `[` and `]` enclose optional items.
Angle brackets `<` and `>` indicate non-terminals.
Vertical bar `|` separates alternatives.
`<id>` is an alphanumeric char sequence.
Terminals are given in double quotes.

</details>

Bulk patterns are supported in both `labels` and `structuredMetadata` sections:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <labels>
        * = %%mdc{vendor}
    </labels>
    <structuredMetadata>
        * != %%mdc{vendor}
        * = %%kvp
    </structuredMetadata>
    ...
</appender>
```

In this example, MDC entry with key "vendor" (if it's defined) becomes a label.
All other MDC entries as well as all KVP entries are sent to Loki as a structured metadata.

## Adding dynamic labels using SLF4J Markers

In classic Logback, markers are typically used to [filter](https://logback.qos.ch/manual/filters.html#TurboFilter) log records.
With Loki4j you can also use markers to set Loki labels (or structured metadata) dynamically for any particular log message.

First, you need to make Loki4j scan markers attached to each log event by enabling `readMarkers` flag:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <readMarkers>true</readMarkers>
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
