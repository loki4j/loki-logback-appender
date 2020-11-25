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
    public void testApacheJsonSlowSend() throws Exception {
        var label = "testApacheJsonSlowSend";
        var appender = apacheHttpAppender(urlPush);
        appender.setBatchSize(500);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(jsonEncoder(false, label));

        var events = generateEvents(1000, 2000);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));

        assertTrue(true);
    }

}
