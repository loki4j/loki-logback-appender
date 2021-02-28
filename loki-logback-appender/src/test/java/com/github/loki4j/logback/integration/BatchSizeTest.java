package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import com.github.loki4j.testkit.categories.IntegrationTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchSizeTest {

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
    public void testApacheJsonMaxBytesSend() throws Exception {
        var label = "testApacheJsonMaxBytesSend";
        var encoder = jsonEncoder(false, label);
        var sender = apacheHttpSender(urlPush);
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 1000, encoder, sender);

        var events = generateEvents(5_000, 2000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufMaxBytesSend() throws Exception {
        var label = "testJavaProtobufMaxBytesSend";
        var encoder = protobufEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(5_000, 1000, encoder, sender);

        var events = generateEvents(5_000, 2000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

}
