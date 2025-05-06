package com.github.loki4j.logback.integration;

import com.github.loki4j.testkit.categories.CIOnlyTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.github.loki4j.logback.Generators.*;

public class MultitenantSupportTest {

    private static String urlBase = "http://localhost:3110/loki/api/v1";
    private static String urlPush = urlBase + "/push";
    private static String tenant = "tenantX";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startLokiClient() {
        client = new LokiTestingClient(urlBase, tenant);
    }

    @AfterClass
    public static void stopLokiClient() {
        client.close();
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testJavaJsonSendWithTenant() throws Exception {
        var label = "testJavaJsonSendWithTenant";
        var sender = javaHttpSender(urlPush);
        sender.setTenantId(tenant);
        var appender = jsonAppender(label, batch(10, 1000), sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender);
    }

    @Test
    @Category({CIOnlyTests.class})
    public void testApacheProtobufSendWithTenant() throws Exception {
        var label = "testApacheProtobufSendWithTenant";
        var sender = apacheHttpSender(urlPush);
        sender.setTenantId(tenant);
        var appender = protoAppender(label, batch(10, 1000), sender);

        var events = generateEvents(20, 20);
        client.testHttpSend(label, events, appender);
    }
}
