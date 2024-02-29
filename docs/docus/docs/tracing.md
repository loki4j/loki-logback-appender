---
id: tracing
title: Tracing and debugging
sidebar_label: Tracing
---

## Verbose flag

By default, Loki4j logs only errors to stderr.
You can make Loki4j print out more details on the encoding/sending process by setting the `verbose` flag to `true` in your `logback.xml`:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <verbose>true</verbose>
</appender>
```

However, it is not recommended to have a `verbose` flag enabled on a regular basis.

## Tracing mode

If you've encountered an issue that can not be properly traced in the verbose mode, you can enable the trace mode.
In the trace mode, Loki4j will be printing out a lot of its internal behavior, so this can be a good option for deep debugging.

### When to use

Tracing mode allows you to observe Loki4j's internal logic in more detail.
It can be helpful in case of:

- Batching issues: some log records are lost or not sent to Loki
- Encoding issues: some labels are missing, incorrect message formatting, or Loki sends 4xx error statuses and complains about batch format
- Delivery issues: Loki4j suddenly stopped sending logs, unhandled/unexpected exceptions in stderr

Tracing mode is unlikely to help with the following:

- Connectivity/access issues - check the correctness of your HTTP configuration instead 

**Warning! Trace mode may cause significant performance penalties. It's not intended for production usage.**

### Preparations

Typically, you would want to reduce the amount of data printed out while Loki4j is in trace mode.

1. Make sure your main logs are not printed to stderr
2. Disable logging for all Java packages except the problematic ones (if any)
3. If possible, reduce Loki4j batch size to minimal using `batchMaxItems` setting

### Enabling tracing mode

To enable the trace mode you need to set Java's system property `loki4j.trace=AsyncBufferPipeline`.
This can be done in various ways depending on the frameworks you use.
One of the simplest ways is to provide it in the command line when starting your app:

```
java -Dloki4j.trace=AsyncBufferPipeline -jar my-app.jar
```

Typically, there would be a lot of information, so you might want to redirect stderr to file:

```
java -Dloki4j.trace=AsyncBufferPipeline -jar my-app.jar 2>loki4j-trace.log
```

Feel free to attach trace logs to the issue if you have found a bug in Loki4j.