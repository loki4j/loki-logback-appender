package com.github.loki4j.logback;

import java.util.Random;

import com.github.loki4j.common.HttpHeaders;
import com.github.loki4j.testkit.dummy.LokiHttpServerMock;

import static com.github.loki4j.logback.Generators.*;
import static com.github.loki4j.logback.Loki4jAppenderTest.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class JavaHttpAppenderTest {

    private static int testPort = -1;
    private static LokiHttpServerMock mockLoki;
    private static String url;

    @BeforeClass
    public static void startMockLoki() {
        testPort = 20_000 + new Random().nextInt(10_000);
        mockLoki = lokiMock(testPort);
        mockLoki.start();

        url = String.format("http://localhost:%s/loki/api/v1/push", testPort);
    }

    @AfterClass
    public static void stopMockLoki() {
        mockLoki.stop();
    }

    @Before
    public void resetMockLoki() {
        mockLoki.reset();
    }

    @Test
    public void testJavaHttpSend() {
        withAppender(appender(3, 1000L, defaultToStringEncoder(), javaHttpSender(url)), a -> {
            a.append(events[0]);
            a.append(events[1]);
            assertTrue("no batches before batchSize reached", mockLoki.lastBatch == null);

            a.append(events[2]);
            a.waitAllAppended();
            assertEquals("http send", expected, new String(mockLoki.lastBatch));
            return null;
        });
    }

    @Test
    public void testJavaHttpSendWithTenantHeader() {
        var sender = javaHttpSender(url);
        sender.setTenantId("tenant1");
        withAppender(appender(3, 1000L, defaultToStringEncoder(), sender), a -> {
            a.append(events);
            a.waitAllAppended();
            assertEquals("http send", expected, new String(mockLoki.lastBatch));
            assertTrue(mockLoki.lastHeaders
                .get(HttpHeaders.X_SCOPE_ORGID)
                .contains("tenant1"));
            return null;
        });
    }

}
