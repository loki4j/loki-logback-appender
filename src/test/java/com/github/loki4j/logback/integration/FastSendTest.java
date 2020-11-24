package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FastSendTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
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


    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var appender = apacheHttpAppender(urlPush);
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(jsonEncoder(false, label));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label), 0L);

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var appender = javaHttpAppender(urlPush);
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(protobufEncoder(false, label));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label), 0L);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheProtobufFastSend() throws Exception {
        var label = "testApacheProtobufFastSend";
        var appender = apacheHttpAppender(urlPush);
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(protobufEncoder(false, label));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label), 0L);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufFastSend() throws Exception {
        var label = "testJavaProtobufFastSend";
        var appender = javaHttpAppender(urlPush);
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(jsonEncoder(false, label));

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label), 0L);
    }

}
