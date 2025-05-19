package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.logback.Loki4jAppender;
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
        var appender = appender(label, batch(10, 1000), http(urlPush, jsonFormat(), javaSender()));

        var events = generateEvents(1, 10);
        client.testHttpSend(label, events, appender);
    }


    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var appender = appender(label, batch(10, 1000), http(urlPush, jsonFormat(), apacheSender()));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var appender = appender(label, batch(10, 1000), http(urlPush, jsonFormat(), javaSender()));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufFastSend() throws Exception {
        var label = "testApacheProtobufFastSend";
        var appender = appender(label, batch(10, 1000), http(urlPush, protobufFormat(), apacheSender()));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufFastSend() throws Exception {
        var label = "testJavaProtobufFastSend";
        var appender = appender(label, batch(10, 1000), http(urlPush, protobufFormat(), javaSender()));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutJsonFastSend() throws Exception {
        var label = "testJsonLayoutJsonFastSend";
        var appender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(10, 1000),
            http(urlPush, jsonFormat(), javaSender()));

        var events = generateEvents(1000, 10);
        var expectedAppender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(events.length, 10L),
            http(null));
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJsonLayoutProtobufFastSend() throws Exception {
        var label = "testJsonLayoutProtobufFastSend";
        var appender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(10, 1000),
            http(urlPush, protobufFormat(), javaSender()));

        var events = generateEvents(1000, 10);
        var expectedAppender = appender(
            "service_name=my-app\ntest=" + label,
            Loki4jAppender.DISABLE_SMD_PATTERN,
            jsonMsgLayout(),
            batch(events.length, 10L),
            http(null));
        client.testHttpSend(label, events, appender, expectedAppender, events.length, 10L);
    }

}
