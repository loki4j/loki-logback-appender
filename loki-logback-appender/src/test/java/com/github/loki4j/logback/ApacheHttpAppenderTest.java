package com.github.loki4j.logback;

import java.util.Random;

import com.github.loki4j.testkit.dummy.LokiHttpServerMock;

import static com.github.loki4j.logback.Generators.*;
import static com.github.loki4j.logback.Loki4jAppenderTest.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApacheHttpAppenderTest {

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
    public void testApacheHttpSend() {
        withAppender(appender(3, 1000L, defaultToStringEncoder(), apacheHttpSender(url)), a -> {
            a.appendAndWait(events[0], events[1]);
            assertTrue("no batches before batchSize reached", mockLoki.lastBatch == null);

            a.appendAndWait(events[2]);
            assertEquals("http send", expected, new String(mockLoki.lastBatch));

            return null;
        });
    }

}
