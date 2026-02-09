package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class ParSendTest {

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
    public void testJavaJsonParSend() throws Exception {
        var events = generateEvents(500, 500);

        var parFactor = 8;
        ExecutorService tp = Executors.newFixedThreadPool(parFactor);
        var fs = new CompletableFuture[parFactor];
        for (int i = 0; i < parFactor; i++) {
            var idx = i;
            var label = "testJavaJsonParSend" + idx;
            var appender = appender(label, batch(10, 150_000), http(urlPush, jsonFormat(), javaSender()));

            fs[i] = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        client.testHttpSend(
                            label,
                            events,
                            appender,
                            null,
                            5,
                            (parFactor - idx) + 500L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return "label";
                }, tp);
        }
        for (int i = 0; i < fs.length; i++) {
            assertEquals("label", fs[i].get());
        }
    }

    @Test
    public void testApacheJsonParSend() throws Exception {
        var events = generateEvents(500, 500);

        var parFactor = 8;
        ExecutorService tp = Executors.newFixedThreadPool(parFactor);
        var fs = new CompletableFuture[parFactor];
        for (int i = 0; i < parFactor; i++) {
            var idx = i;
            var label = "testApacheJsonParSend" + idx;
            var appender = appender(label, batch(10, 150_000), http(urlPush, jsonFormat(), apacheSender()));

            fs[i] = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        client.testHttpSend(
                            label,
                            events,
                            appender,
                            null,
                            5,
                            (parFactor - idx) + 500L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return "label";
                }, tp);
        }
        for (int i = 0; i < fs.length; i++) {
            assertEquals(fs[i].get(), "label");
        }
    }
}
