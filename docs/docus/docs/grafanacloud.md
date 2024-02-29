---
id: grafanacloud
title: Sending logs to Grafana Cloud
sidebar_label: Grafana Cloud
---

This example shows how to send log records to the hosted Loki service (e.g., Grafana Cloud).
We would need to specify the credentials and increase the request timeout to 15s.
Also, Grafana Cloud limit for batch length is 65536 bytes, while for standalone Loki it's 4 MB by default,
so we need to specify this explicitly.

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <batchMaxBytes>65536</batchMaxBytes>
    <http>
        <url>https://logs-prod-us-central1.grafana.net/loki/api/v1/push</url>
        <auth>
            <username>example_username</username>
            <password>example_api_token</password>
        </auth>
        <requestTimeoutMs>15000</requestTimeoutMs>
    </http>
    <format>
        <label>
            <pattern>app=my-app</pattern>
        </label>
        <message>
            <pattern>l=%level c=%logger{20} t=%thread | %msg %ex</pattern>
        </message>
    </format>
</appender>
```