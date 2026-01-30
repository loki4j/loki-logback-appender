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
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterClass
    public static void stopLokiClient() {
        client.close();
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonMaxBytesSend() throws Exception {
        var label = "testApacheJsonMaxBytesSend";

        var http = http(urlPush, jsonFormat(), apacheSender());
        http.setRequestTimeoutMs(30_000L);
        var batch = batch(5_000, 1000);
        batch.setSendQueueMaxBytes(100 * 1024 * 1024);
        var appender = appender(label, batch, http);

        var events = generateEvents(5_000, 2000);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaProtobufMaxBytesSend() throws Exception {
        var label = "testJavaProtobufMaxBytesSend";

        var http = http(urlPush, protobufFormat(), javaSender());
        http.setRequestTimeoutMs(30_000L);
        var batch = batch(5_000, 1000);
        batch.setSendQueueMaxBytes(100 * 1024 * 1024);
        var appender = appender(label, batch, http);

        var events = generateEvents(5_000, 2000);
        client.testHttpSend(label, events, appender);

        assertTrue(true);
    }

}
