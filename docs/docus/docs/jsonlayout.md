---
id: jsonlayout
title: Configuring JSON message layout
sidebar_label: JSON Message Layout
---

## When to use JSON message layout

If all you need is to send log messages to Loki along with some metadata (e.g., class name, thread name, log level, MDC, KVP), you can just set up proper [labels and structured metadata](labels.md) and use the default plain text layout.
Thus, you do not need JSON message layout in most of the cases.

Although Loki4j supports JSON layout for log messages, it is not Loki's native format for logs.
Producing and parsing log lines in JSON imposes performance penalties and extra cost comparing to the plain text layout.
Also worth mentioning that JSON is harder to read for humans than the plain text as well.

Having that said, Loki4j's JSON message layout has no alternatives if you:

- need nested structures with custom serialization in your metadata;
- have tools in your logging stack (apart from Loki) that require logstash-like JSON format.

Now, if you are still sure JSON layout is the right option for you, please proceed to the next section.

## Enabling the JSON layout

You can enable JSON layout for log messages by specifying a corresponding `class` attribute for a `message` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    ...
    <message class="com.github.loki4j.logback.JsonLayout" />
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

For more insights on how `JsonEventWriter` works, please check the next section.

## Serializing an arbitrary object

Our recommendation is to use plain structure of the log message (i.e., no nested objects), and only some basic types for fields (e.g.: string, number, boolean).
Following this recommendation will give you both maximum performance and simplicity.

However, Logback allows you to attach arbitrary objects to you log record as key-value pairs:

```java
log.atInfo().setMessage("Test KVP")
    .addKeyValue("flow", "ingest")
    .addKeyValue("msgId", 123456)
    .addKeyValue("bool", true)
    .addKeyValue("list", List.of(0, 1, 2))
    .addKeyValue("date", ZonedDateTime.now())
    .addKeyValue("obj", new TestJsonKvData(1001L, "admin", UUID.randomUUID()))
    .log();
```

Loki4j uses fast and compact JSON serialization algorithm that is [3.5x](https://github.com/loki4j/loki-logback-appender/pull/210#issue-2113540494) faster than logstash-logback-encoder backed by jackson.
But that speed comes with the cost: no runtime reflection is used, no complex type conversion is implemented.
Which means we don't provide a built-in mechanism to serialize arbitrary object into JSON right away.
Instead, we offer you several options, so you can choose the one that better fits your use case.

Loki4j has `KeyValuePairsJsonProvider`, that by default uses `writeObjectField()` for all key-value pairs.

#### Default approach: `writeObjectField()`

Method `writeObjectField()` in class `JsonEventWriter` has the following logic:

- if value is `String`, it's rendered as JSON string: `"flow":"ingest"`, similar to `writeStringField()`;
- if value is `Integer` or `Long`, it's rendered as JSON number: `"msgId":123456`, similar to `writeNumericField()`;
- if value is `Boolean`, it's rendered as JSON boolean: `"bool":true`;
- if value is `Iterable`, it's rendered as JSON array, `writeObjectField()` to render each element: `"list":[0,1,2]`, similar to `writeArrayField()`;
- if value is `RawJsonString`, its value is rendered as raw JSON without any escaping (see the section below), similar to `writeRawJsonField()`;
- for any other type of value, the result of `toString()` is rendered as JSON string: `"obj":"TestJsonKvData@1de5f259"`.

#### Direct access to `JsonEventWriter`

If you write a custom provider, `JsonEventWriter` is exposed to you directly.
You can also configure `KeyValuePairsJsonProvider` to intercept writer calls as well by setting `fieldSerializer`:

```xml
<message class="com.github.loki4j.logback.JsonLayout">
    ...
    <kvp>
        <fieldSerializer class="io.my.TestFieldSerializer" />
    </kvp>
</message>
```

Your custom serializer must implement interface `JsonFieldSerializer<Object>`:

```java
import com.github.loki4j.logback.json.JsonEventWriter;
import com.github.loki4j.logback.json.JsonFieldSerializer;

public class TestFieldSerializer implements JsonFieldSerializer<Object> {
    @Override
    public void writeField(JsonEventWriter writer, String fieldName, Object fieldValue) {
        if (fieldValue instanceof TestJsonKvData) {
            writer.writeCustomField(fieldName, w -> {
                    var td = (TestJsonKvData)fieldValue;
                    w.writeBeginObject();
                    w.writeObjectField("userId", td.userId);
                    w.writeFieldSeparator();
                    w.writeObjectField("userName", td.userName);
                    w.writeFieldSeparator();
                    w.writeObjectField("sessionId", td.sessionId);
                    w.writeEndObject();
                }
            );
        } else {
            writer.writeObjectField(fieldName, fieldValue);
        }
    }
}
```

Instead of `"obj":"TestJsonKvData@1de5f259"`, now you will see `"obj":{"userId":1001,"userName":"admin","sessionId":"92a26b23-9f10-47d8-bb0a-df2b9b15c374"}`.

#### `RawJsonString`

If for some reason you can not statically define serialization algorithm for your objects, you can use reflection-based frameworks and put the resulting string into `RawJsonString`.
Writer will render it as is, without any escaping.

```java
import com.github.loki4j.logback.json.RawJsonString;

public class TestFieldSerializer implements JsonFieldSerializer<Object> {
    @Override
    public void writeField(JsonEventWriter writer, String fieldName, Object fieldValue) {
        writer.writeObjectField(fieldName, new RawJsonString(MyJson.serialize(fieldValue)));
        // or
        writer.writeRawJsonField(fieldName, MyJson.serialize(fieldValue));
    }
}
```

It is your responsibility to make sure that you always put valid JSON into `RawJsonString`.

#### Custom message layout

You can use any implementation of `Layout<ILoggingEvent>` as a message layout, including third-party JSON layouts:

```xml
<message class="net.logstash.logback.layout.LoggingEventCompositeJsonLayout">
    <!-- your logstash configuration goes here -->
</message>
```

Please note that Loki4j provides **no support** for any third-party components.
