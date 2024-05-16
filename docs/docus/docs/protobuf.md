---
id: protobuf
title: Switching to Protobuf format 
sidebar_label: Protobuf Support
---

By default, Loki4j uses `JsonEncoder` that converts log batches into JSON format specified by Loki API.
This encoder does not use any extra libs for JSON generation.

If you want to use `ProtobufEncoder`, you need to add the following dependency to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>com.github.loki4j</groupId>
    <artifactId>loki-protobuf</artifactId>
    <version>0.0.2_pb4.26.0</version>
</dependency>
```

<!--Gradle-->

```groovy
implementation 'com.github.loki4j:loki-protobuf:0.0.2_pb4.26.0'
```
<!--END_DOCUSAURUS_CODE_TABS-->

This library contains pre-generated encoders for Loki Protobuf format along with the proper version of Protobuf runtime and Snappy as transitive dependencies.

Then you can explicitly specify `ProtobufEncoder` by setting `class` attribute for `format` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <format class="com.github.loki4j.logback.ProtobufEncoder">
        ...
    </format>
</appender>
```

### Selecting a version of Protobuf

If your project already depends on a certain version of Protobuf, you will have to use a proper version of `loki-protobuf` as well.

You can change the Protobuf version bundled with `loki-protobuf` by modifying the `_pbX.Y.0` part.
The list of supported Protobuf versions is available in [PB-VERSION file](https://github.com/loki4j/loki-logback-appender/blob/main/loki-protobuf/PB-VERSION).

If the version you need is not listed in there, consider the following options:

- Add the version you need to PB-VERSION file and create a PR to Loki4j. Once merged, this will add one more publishing configuration to `loki-protobuf`, and the new Protobuf version will be available to everyone

- Put Loki-specific `.proto` files inside your project and generate corresponding Java files yourself. This option is considered advanced, so you should be able to figure out all the details you might need from the `loki-protobuf`'s [source code](https://github.com/loki4j/loki-logback-appender/tree/main/loki-protobuf)

