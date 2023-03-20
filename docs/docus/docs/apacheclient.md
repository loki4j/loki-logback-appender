---
id: apacheclient
title: Using Apache HttpClient
sidebar_label: Apache HttpClient
---

By default Loki4j uses `JavaHttpSender`, backed by `java.net.http.HttpClient` available in Java 11 and later.
This sender does not require any extra dependencies.
So it should be a good fit for the most users.

However, you may want to switch to `ApacheHttpSender`, backed by `org.apache.http.client.HttpClient` available for Java 8+ projects.
In this case you need to ensure you have added the required dependencies to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>
```

<!--Gradle-->

```groovy
implementation 'org.apache.httpcomponents:httpclient:4.5.14'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you can explicitly specify `ApacheHttpSender` by setting `class` attribute for `http` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http class="com.github.loki4j.logback.ApacheHttpSender">
        ...
    </http>
</appender>
```

`ApacheHttpSender` shares most of the settings with `JavaHttpSender`.
However, there are some specific settings available only for `ApacheHttpSender`:

Setting|Default|Description
-------|-------|-----------
http.maxConnections|1|Maximum number of HTTP connections to keep in the pool
http.connectionKeepAliveMs|120000|A duration of time in milliseconds which the connection can be safely kept idle for later reuse. This value should not be greater than `server.http-idle-timeout` in your Loki config
