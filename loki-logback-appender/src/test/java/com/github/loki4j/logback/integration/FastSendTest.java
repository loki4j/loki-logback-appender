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
        var sender = javaHttpSender(urlPush);
        var appender = jsonAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1, 10);
        client.testHttpSend(label, events, appender);
    }


    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var sender = apacheHttpSender(urlPush);
        var appender = jsonAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var sender = javaHttpSender(urlPush);
        var appender = jsonAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufFastSend() throws Exception {
        var label = "testApacheProtobufFastSend";
        var sender = apacheHttpSender(urlPush);
        var appender = protoAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufFastSend() throws Exception {
        var label = "testJavaProtobufFastSend";
        var sender = javaHttpSender(urlPush);
        var appender = protoAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutJsonFastSend() throws Exception {
        var label = "testJsonLayoutJsonFastSend";
        var sender = javaHttpSender(urlPush);
        var appender = jsonAppender("service_name=my-app\ntest=" + label, null, new JsonLayout(), batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        var expectedAppender = jsonAppender("service_name=my-app\ntest=" + label, null, new JsonLayout(), batch(events.length, 10L), sender);
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutProtobufFastSend() throws Exception {
        var label = "testJsonLayoutProtobufFastSend";
        var sender = javaHttpSender(urlPush);
        var appender = protoAppender("service_name=my-app\ntest=" + label, null, new JsonLayout(), batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        var expectedAppender = jsonAppender("service_name=my-app\ntest=" + label, null, new JsonLayout(), batch(events.length, 10L), sender);
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

}
