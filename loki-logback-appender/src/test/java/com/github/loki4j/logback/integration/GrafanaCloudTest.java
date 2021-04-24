package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.logback.AbstractHttpSender;
import com.github.loki4j.testkit.categories.CIOnlyTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GrafanaCloudTest {

    private static String urlBase = "https://logs-prod-us-central1.grafana.net/loki/api/v1";
    private static String urlPush = urlBase + "/push";

    private static String username = System.getenv("GRAFANA_CLOUD_USERNAME");
    private static String password = System.getenv("GRAFANA_CLOUD_PASSWORD");

    private static LokiTestingClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiTestingClient(urlBase, username, password);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }

    public static AbstractHttpSender authorize(AbstractHttpSender sender) {
        var auth = new AbstractHttpSender.BasicAuth();
        auth.setUsername(username);
        auth.setPassword(password);
        sender.setAuth(auth);
        sender.setRequestTimeoutMs(30_000L);
        return sender;
    }


    @Test
    @Category({CIOnlyTests.class})
    public void testApacheJsonCloud() throws Exception {
        var label = "testApacheJsonCloud";
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
        var label = "testJavaJsonCloud";
        var encoder = jsonEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(20, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheProtobufCloud() throws Exception {
        var label = "testApacheProtobufCloud";
        var encoder = protobufEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(50, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaProtobufCloud() throws Exception {
        var label = "testJavaProtobufCloud";
        var encoder = protobufEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(100, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheJsonMaxBytesSend() throws Exception {
        var label = "testApacheJsonMaxBytesCloud-" + System.currentTimeMillis();
        var encoder = protobufEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 100, encoder, sender);
        appender.setBatchMaxBytes(65536);
        appender.setSendQueueMaxBytes(100 * 1024 * 1024);
        //appender.setVerbose(false);

        var events = generateEvents(1000, 1000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaProtobufMaxBytesSend() throws Exception {
        var label = "testJavaProtobufMaxBytesCloud-" + System.currentTimeMillis();
        var encoder = protobufEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 100, encoder, sender);
        appender.setBatchMaxBytes(65536);
        appender.setSendQueueMaxBytes(100 * 1024 * 1024);
        //appender.setVerbose(false);

        var events = generateEvents(1000, 1000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

}
