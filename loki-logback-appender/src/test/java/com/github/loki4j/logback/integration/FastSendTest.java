package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.testkit.categories.IntegrationTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FastSendTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";
    private static String tenant = "tenantX";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }


    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var encoder = jsonEncoder(false, label);
        var sender = apacheHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var encoder = protobufEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufFastSend() throws Exception {
        var label = "testApacheProtobufFastSend";
        var encoder = protobufEncoder(false, label);
        var sender = apacheHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufFastSend() throws Exception {
        var label = "testJavaProtobufFastSend";
        var encoder = jsonEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSendWithTenant() throws Exception {
        var label = "testJavaJsonFastSendWithTenant";
        var encoder = protobufEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        sender.setTenantId(tenant);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }


}
