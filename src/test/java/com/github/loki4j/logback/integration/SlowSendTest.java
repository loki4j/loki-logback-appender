package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SlowSendTest {

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
        var events = generateEvents(10_000, 500);

        var str = Stream.of(1, 2, 3, 4, 5, 6, 7, 8);
        str.parallel().forEach(idx -> {
            var label = "testApacheJsonSlowSend" + idx;
            var appender = apacheHttpAppender(urlPush);
            appender.setBatchSize(1000);
            appender.setBatchTimeoutMs(150_000);
            appender.setEncoder(jsonEncoder(false, label));
            //appender.setMaxConnections(100);
            //appender.setHttpThreads(1);

            
            try {
                client.testHttpSend(label, events, appender, jsonEncoder(false, label), 100L);
            } catch (Exception e) {
                new RuntimeException(e);
            }
        });

        assertTrue(true);
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonSend() throws Exception {
        var events = generateEvents(3, 20);

        var label = "testApacheJsonSend";
        var appender = apacheHttpAppender(urlPush);
        appender.setBatchSize(2);
        appender.setBatchTimeoutMs(150_000);
        appender.setEncoder(jsonEncoder(false, label));
        
        client.testHttpSend(label, events, appender, jsonEncoder(false, label), 1000L);
    }

}
