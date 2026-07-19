package com.github.loki4j.logback;

import java.util.Random;

import com.github.loki4j.client.http.HttpHeader;
import com.github.loki4j.testkit.dummy.LokiHttpServerMock;
import com.github.loki4j.testkit.dummy.StringPayload;

import static com.github.loki4j.logback.Generators.*;
import static com.github.loki4j.logback.Loki4jAppenderTest.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApacheHttp5AppenderTest {

    private static int testPort = -1;
    private static LokiHttpServerMock mockLoki;
    private static String url;

    @BeforeAll
    public static void startMockLoki() {
        testPort = 20_000 + new Random().nextInt(10_000);
        mockLoki = lokiMock(testPort);
        mockLoki.start();

        url = String.format("http://localhost:%s/loki/api/v1/push", testPort);
    }

    @AfterAll
    public static void stopMockLoki() {
        mockLoki.stop();
    }

    @BeforeEach
    public void resetMockLoki() {
        mockLoki.reset();
    }

    @Test
    public void testApacheHttpOffHeapSend() {
        withAppender(appender(batch(3, 1000L), http(url, stringFormat(), apache5Sender())), a -> {
            a.append(events[0]);
            a.append(events[1]);
            assertTrue(mockLoki.lastBatch == null, "no batches before batchSize reached");

            a.append(events[2]);
            a.waitAllAppended();
            assertEquals(expected, StringPayload.parse(mockLoki.lastBatch), "http send");

            return null;
        });
    }

    @Test
    public void testApacheHttpOnHeapSend() {
        var appender = appender(batch(3, 1000L), http(url, stringFormat(), apache5Sender()));
        appender.getBatch().setUseDirectBuffers(false);
        withAppender(appender, a -> {
            a.append(events[0]);
            a.append(events[1]);
            assertTrue(mockLoki.lastBatch == null, "no batches before batchSize reached");

            a.append(events[2]);
            a.waitAllAppended();
            assertEquals(expected, StringPayload.parse(mockLoki.lastBatch), "http send");

            return null;
        });
    }

    @Test
    public void testApacheHttpSendWithTenantHeader() {
        var http = http(url, stringFormat(), apache5Sender());
        http.setTenantId("tenant1");
        withAppender(appender(batch(3, 1000L), http), a -> {
            a.append(events);
            a.waitAllAppended();
            assertEquals(expected, StringPayload.parse(mockLoki.lastBatch), "http send");
            assertTrue(mockLoki.lastHeaders
                .get(HttpHeader.X_SCOPE_ORGID)
                .contains("tenant1"));
            return null;
        });
    }

}
