---
id: jsonlayout
title: Configuring JSON message layout
sidebar_label: JSON Message Layout
---

## When to use JSON message layout

JSON is not Loki's native format for logs.
Producing and parsing log lines in JSON format imposes significant performance penalties comparing to the plain text layout.
JSON is harder to read for humans as well.

Having that said, Loki4j's JSON message layout probably has no alternatives if you:

- can not specify what exact keys to extract from MDC or KV;
- need nested structures in your metadata;
- have to export your log messages from Loki to another tool that requires JSON.

If you are not dealing with something from the list above or similar,
we recommend you to take a look at other ways for attaching metadata to log records: [labels](labels.md) and [structured metadata](metadata.md).
Both of them work perfectly with plain text layout.

Now, if you are still sure JSON layout is the right option for you, please proceed to the next section.

## Enabling the JSON layout

You can enable JSON layout for log messages by specifying a corresponding `class` attribute for a `message` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format>
        ...
        <message class="com.github.loki4j.logback.JsonLayout" />
    </format>
    ...
</appender>
```

That's it! After running your application, you should see your log messages are sent to Loki as JSON objects:

```json
{"timestamp_ms":1707947657247,"logger_name":"io.my.App","level":"INFO","thread_name":"main","message":"42"}
```

## Fine-tuning the standard providers

Loki4j comes with a predefined set of standard JSON providers.
Each of them can write zero, one, or more fields to the resulting JSON object.
All standard provides are enabled by default, but you can disable them using the `enabled` setting:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <timestamp>
        <enabled>false</enabled>
    </timestamp>
</message>
```

This will remove a timestamp from the log record:

```json
{"logger_name":"io.my.App","level":"INFO","thread_name":"main","message":"42"}
```

Most of the providers have a `fieldName` setting that allows you to customize the JSON field name they will write to:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <loggerName>
        <fieldName>class</fieldName>
    </loggerName>
</message>
```

Applying this configuration will give you:

```json
{"timestamp_ms":1707947657247,"class":"io.my.App","level":"INFO","thread_name":"main","message":"42"}
```

Some providers have their own specific properties.
For example, you can configure the MDC provider to include only specified keys in the log record:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <mdc>
        <include>myKey1</include>
        <include>myKey2</include>
    </mdc>
</message>
```

Please check the [reference](configuration#json-message-layout) for a complete list of standard providers and their settings.

## Custom JSON providers

If standard providers don't cover all your needs, consider creating your own provider.
All providers must implement the `com.github.loki4j.logback.json.JsonProvider` interface.
The easiest way to build a one-field provider is to derive it from the `AbstractFieldJsonProvider` from the same package.
Here is the example implementation:

```java
public class ConstantProvider extends AbstractFieldJsonProvider {

    public ConstantProvider() {
        setFieldName("constant_name");
    }

    @Override
    protected void writeExactlyOneField(JsonEventWriter writer, ILoggingEvent event) {
        writer.writeStringField(getFieldName(), "constant_value");
    }
}
```

Then, you need to register this new provider in the configuration:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <customProvider class="io.my.ConstantProvider" />
</message>
```

After applying this configuration, you will see the following:

```json
{"timestamp_ms":1707947657247,"logger_name":"io.my.App","level":"INFO","thread_name":"main","message":"42","constant_name":"constant_value"}
```

You can add as many custom providers as you want:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <customProvider class="io.my.ConstantProvider1" />
    <customProvider class="io.my.ConstantProvider2" />
    <customProvider class="io.my.ConstantProvider3" />
</message>
```
