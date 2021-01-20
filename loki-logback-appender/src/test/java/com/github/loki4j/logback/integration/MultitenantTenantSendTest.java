package com.github.loki4j.logback.integration;

import com.github.loki4j.testkit.categories.IntegrationTests;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.assertTrue;

public class MultitenantTenantSendTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";
    private static String tenant = "tenantX";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiTestingClient(urlBase, tenant);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSendWithTenant() throws Exception {
        var label = "testJavaJsonFastSendWithTenant";
        var encoder = protobufEncoder(false, label);
        var sender = javaHttpSender(urlPush);
        sender.setTenantId(tenant);
        var appender = appender(10, 1000, encoder, sender);

        var events = generateEvents(1000, 10);
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }


}
