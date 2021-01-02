package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LargeBatchSendTest {

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
    public void testApacheJsonBatchSend() throws Exception {
        var label = "testApacheJsonBatchSend";
        var layout = jsonLayout(false, label);
        var sender = apacheHttpSender(urlPush);
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(500, 1000, layout, sender);

        var events = generateEvents(1000, 2000);
        client.testHttpSend(label, events, appender, jsonLayout(false, label));

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonBatchSend() throws Exception {
        var label = "testJavaJsonBatchSend";
        var layout = jsonLayout(false, label);
        var sender = javaHttpSender(urlPush);
        sender.setRequestTimeoutMs(30_000L);
        var appender = appender(500, 1000, layout, sender);

        var events = generateEvents(1000, 2000);
        client.testHttpSend(label, events, appender, jsonLayout(false, label));

        assertTrue(true);
    }

}
