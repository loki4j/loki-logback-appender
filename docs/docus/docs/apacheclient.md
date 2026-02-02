---
id: apacheclient
title: Using Apache HttpClient
sidebar_label: Apache HttpClient
---

By default, Loki4j uses `JavaHttpSender`, backed by `java.net.http.HttpClient`.
This sender does not require any extra dependencies.
So, it should be a good fit for most users.

However, you may want to switch to `ApacheHttpSender`, backed by `org.apache.http.client.HttpClient`.
In this case, you need to ensure you have added the required dependencies to your project:

<!--DOCUSAURUS_CODE_TABS-->
<!--Maven-->

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.5.2</version>
</dependency>
```

<!--Gradle-->

```groovy
implementation 'org.apache.httpcomponents.client5:httpclient5:5.6'
```
<!--END_DOCUSAURUS_CODE_TABS-->

Then you can explicitly specify `ApacheHttpSender` by setting `class` attribute for `http.sender` section:

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <sender class="com.github.loki4j.logback.ApacheHttpSender">
            ...
        </sender>
    </http>
    ...
</appender>
```

There are some specific settings available only for `ApacheHttpSender` that you can specify in `sender` section as well:

Setting|Default|Description
-------|-------|-----------
http.sender.maxConnections|1|Maximum number of HTTP connections to keep in the pool
http.sender.connectionKeepAliveMs|120000|A duration of time in milliseconds in which the connection can be safely kept idle for later reuse. This value should not be greater than `server.http-idle-timeout` in your Loki config
