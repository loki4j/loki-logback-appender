package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.loki4j.logback.Loki4jAppender;
import com.github.loki4j.logback.PipelineConfigAppenderBase.BasicAuth;
import com.github.loki4j.logback.PipelineConfigAppenderBase.HttpCfg;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("ci-only")
public class GrafanaCloudTest {

    private static String urlBase = System.getenv("GRAFANA_CLOUD_URL_BASE");
    private static String urlPush = urlBase + "/push";

    private static String username = System.getenv("GRAFANA_CLOUD_USERNAME");
    private static String password = System.getenv("GRAFANA_CLOUD_PASSWORD");
    private static String extraLabel = System.getenv("GRAFANA_CLOUD_EXTRA_LABEL");

    private static LokiTestingClient client;

    @BeforeAll
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase, username, password);
    }

    @AfterAll
    public static void stopLokiClient() {
        client.close();
    }

    private static String label(String l) {
        var ts = System.currentTimeMillis();
        return extraLabel == null
            ? (l + "-" + ts)
            : (l + extraLabel + "-" + ts);
    }

    private static HttpCfg authorize(HttpCfg sender) {
        var auth = new BasicAuth();
        auth.setUsername(username);
        auth.setPassword(password);
        sender.setAuth(auth);
        sender.setRequestTimeoutMs(30_000L);
        return sender;
    }

    @Test
    public void testApacheJsonOneEventCloud() throws Exception {
        var label = label("testApacheJsonOneEventCloud");
        var http = authorize(http(urlPush, jsonFormat(), apacheSender()));
        var appender = appender(label, batch(10, 1000), http);

        var events = generateEvents(1, 20);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    public void testApacheJsonCloud() throws Exception {
        var label = label("testApacheJsonCloud");
        var http = authorize(http(urlPush, jsonFormat(), apacheSender()));
        var appender = appender(label, batch(10, 1000), http);

        var events = generateEvents(20, 20);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    public void testJavaJsonCloud() throws Exception {
        var label = label("testJavaJsonCloud");
        var http = authorize(http(urlPush, jsonFormat(), javaSender()));
        var appender = appender(label, batch(10, 1000), http);

        var events = generateEvents(20, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    public void testApacheProtobufCloud() throws Exception {
        var label = label("testApacheProtobufCloud");
        var http = authorize(http(urlPush, protobufFormat(), apacheSender()));
        var appender = appender(label, batch(10, 1000), http);

        var events = generateEvents(50, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    public void testJavaProtobufCloud() throws Exception {
        var label = label("testJavaProtobufCloud");
        var http = authorize(http(urlPush, protobufFormat(), javaSender()));
        var appender = appender(label, batch(10, 1000), http);

        var events = generateEvents(100, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    public void testJsonLayoutJavaJsonCloud() throws Exception {
        var label = label("testJsonLayoutJavaJsonCloud");
        var http = authorize(http(urlPush, jsonFormat(), javaSender()));
        var appender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(10, 1000),
            http);

        var events = generateEvents(20, 10);
        var expectedAppender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(events.length, 10L),
            http(jsonFormat(),
            null));
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

    @Test
    public void testJsonLayoutApacheProtobufCloud() throws Exception {
        var label = label("testJsonLayoutApacheProtobufCloud");
        var http = authorize(http(urlPush, protobufFormat(), apacheSender()));
        var appender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(10, 1000),
            http);

        var events = generateEvents(50, 10);
        var expectedAppender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(events.length, 10L),
            http(jsonFormat(), null));
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

    @Test
    public void testApacheJsonMaxBytesSend() throws Exception {
        var label = label("testApacheJsonMaxBytesSendCloud");
        var http = authorize(http(urlPush, jsonFormat(), apacheSender()));
        http.setRequestTimeoutMs(30_000L);
        var batch = batch(5_000, 1000);
        batch.setMaxBytes(65536);
        var appender = appender(label, batch, http);
        appender.setVerbose(false);

        var events = generateEvents(1000, 100);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    @Disabled("Disabled due to unpredictable stream sharding on Grafana Cloud Loki side")
    public void testJavaProtobufMaxBytesSend() throws Exception {
        var label = label("testJavaProtobufMaxBytesSendCloud");
        var http = authorize(http(urlPush, protobufFormat(), javaSender()));
        http.setRequestTimeoutMs(30_000L);
        var batch = batch(5_000, 1000);
        batch.setMaxBytes(65536);
        var appender = appender(label, batch, http);
        appender.setVerbose(false);

        var events = generateEvents(1000, 1000);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

}
