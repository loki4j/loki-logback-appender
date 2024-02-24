---
id: jsonlayout
title: Configuring JSON Message Layout
sidebar_label: JSON Message Layout
---

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

That's it! After running your application you should see your log messages are sent to Loki as JSON objects:

```json
{"timestamp_ms":1707947657247,"logger_name":"io.my.App","level":"INFO","thread_name":"main","message":"42"}
```

## Fine-tuning the standard providers

Loki4j comes with a predefined set of standard JSON providers, each of them can write zero, one, or more fields to the resulting JSON object.
All standard provides are enabled by default, but you can disable them using a corresponding setting:

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

Most of the providers have a `fieldName` setting that allows you to customize a JSON field name they will write to:

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

Some providers have their own specific properties, for example you can configure MDC provider to include only specified keys into the log record:

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

If standard providers don't cover all your needs, you may consider creating your own provider.
All providers must implement interface `com.github.loki4j.logback.json.JsonProvider`.
The easiest way to build one field provider is to extend it from `AbstractFieldJsonProvider` from the same package.
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

Then you need to enable this new provider in the configuration:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    <customProvider class="io.my.ConstantProvider" />
</message>
```

After applying this configuration you will see the following:

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
