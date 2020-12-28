package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.logback.AbstractHttpSender;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GrafanaCloudTest {

    private static String urlBase = "https://logs-prod-us-central1.grafana.net/api/prom";
    private static String urlPush = urlBase + "/push";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }

    public static AbstractHttpSender authorize(AbstractHttpSender sender) {
        var auth = new AbstractHttpSender.BasicAuth();
        auth.setUsername(System.getenv("GRAFANA_CLOUD_USERNAME"));
        auth.setPassword(System.getenv("GRAFANA_CLOUD_PASSWORD"));
        sender.setAuth(auth);
        return sender;
    }


    @Test
    @Category({IntegrationTests.class})
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
    @Category({IntegrationTests.class})
    public void testJavaJsonCloud() throws Exception {
        var label = "testJavaJsonCloud";
        var encoder = jsonEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(20, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufCloud() throws Exception {
        var label = "testApacheProtobufCloud";
        var encoder = protobufEncoder(false, label);
        var sender = authorize(apacheHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(50, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufCloud() throws Exception {
        var label = "testJavaProtobufCloud";
        var encoder = protobufEncoder(false, label);
        var sender = authorize(javaHttpSender(urlPush));
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(100, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

}
