package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.logback.AbstractHttpSender;
import com.github.loki4j.logback.JsonLayout;
import com.github.loki4j.testkit.categories.CIOnlyTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GrafanaCloudTest {

    private static String urlBase = System.getenv("GRAFANA_CLOUD_URL_BASE");
    private static String urlPush = urlBase + "/push";

    private static String username = System.getenv("GRAFANA_CLOUD_USERNAME");
    private static String password = System.getenv("GRAFANA_CLOUD_PASSWORD");
    private static String extraLabel = System.getenv("GRAFANA_CLOUD_EXTRA_LABEL");

    private static LokiTestingClient client;

    @BeforeClass
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase, username, password);
    }

    @AfterClass
    public static void stopLokiClient() {
        client.close();
    }

    private static String label(String l) {
        var ts = System.currentTimeMillis();
        return extraLabel == null
            ? (l + "-" + ts)
            : (l + extraLabel + "-" + ts);
    }

    private static AbstractHttpSender authorize(AbstractHttpSender sender) {
        var auth = new AbstractHttpSender.BasicAuth();
        auth.setUsername(username);
        auth.setPassword(password);
        sender.setAuth(auth);
        sender.setRequestTimeoutMs(30_000L);
        return sender;
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheJsonOneEventCloud() throws Exception {
        var label = label("testApacheJsonOneEventCloud");
        var encoder = jsonEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1, 20);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheJsonCloud() throws Exception {
        var label = label("testApacheJsonCloud");
        var encoder = jsonEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(20, 20);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaJsonCloud() throws Exception {
        var label = label("testJavaJsonCloud");
        var encoder = jsonEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(20, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheProtobufCloud() throws Exception {
        var label = label("testApacheProtobufCloud");
        var encoder = protobufEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(50, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaProtobufCloud() throws Exception {
        var label = label("testJavaProtobufCloud");
        var encoder = protobufEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(100, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJsonLayoutJavaJsonCloud() throws Exception {
        var label = label("testJsonLayoutJavaJsonCloud");
        var encoder = jsonEncoder(false, label, new JsonLayout());
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(20, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label, new JsonLayout()));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJsonLayoutApacheProtobufCloud() throws Exception {
        var label = label("testJsonLayoutApacheProtobufCloud");
        var encoder = protobufEncoder(false, label, new JsonLayout());
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(50, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label, new JsonLayout()));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheJsonMaxBytesSend() throws Exception {
        var label = label("testApacheJsonMaxBytesSendCloud");
        var encoder = jsonEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 1000, encoder, sender);
        appender.setBatchMaxBytes(65536);
        appender.setVerbose(false);

        var events = generateEvents(1000, 100);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaProtobufMaxBytesSend() throws Exception {
        var label = label("testJavaProtobufMaxBytesSendCloud");
        var encoder = protobufEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 1000, encoder, sender);
        appender.setBatchMaxBytes(65536);
        appender.setVerbose(false);

        var events = generateEvents(1000, 1000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

}
