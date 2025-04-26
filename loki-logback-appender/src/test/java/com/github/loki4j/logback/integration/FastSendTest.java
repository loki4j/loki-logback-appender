package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.logback.JsonLayout;
import com.github.loki4j.testkit.categories.IntegrationTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FastSendTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterClass
    public static void stopLokiClient() {
        client.close();
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonOneEventSend() throws Exception {
        var label = "testJavaJsonOneEventSend";
        var encoder = jsonEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1, 10);
        client.testHttpSend(label, events, appender);
    }


    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var encoder = jsonEncoder(false, label);
        var sender = apacheHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var encoder = jsonEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufFastSend() throws Exception {
        var label = "testApacheProtobufFastSend";
        var encoder = protobufEncoder(false, label);
        var sender = apacheHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufFastSend() throws Exception {
        var label = "testJavaProtobufFastSend";
        var encoder = protobufEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutJsonFastSend() throws Exception {
        var label = "testJsonLayoutJsonFastSend";
        var encoder = jsonEncoder(false, label, new JsonLayout());
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutProtobufFastSend() throws Exception {
        var label = "testJsonLayoutProtobufFastSend";
        var encoder = protobufEncoder(false, label, new JsonLayout());
        var sender = javaHttpSender(urlPush);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

}
