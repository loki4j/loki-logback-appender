package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

public class BatchSizeTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";

    private static LokiTestingClient client;

    @BeforeAll
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterAll
    public static void stopLokiClient() {
        client.close();
    }

    @Test
    @Tag("com.github.loki4j.testkit.categories.IntegrationTests")
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
    @Tag("com.github.loki4j.testkit.categories.IntegrationTests")
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
