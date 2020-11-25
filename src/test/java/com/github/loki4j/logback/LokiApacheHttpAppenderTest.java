package com.github.loki4j.logback;

import java.util.Random;

import com.github.loki4j.logback.Generators.LokiHttpServerMock;
import static com.github.loki4j.logback.AbstractLoki4jAppenderTest.*;
import static com.github.loki4j.logback.Generators.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import ch.qos.logback.classic.LoggerContext;

public class LokiApacheHttpAppenderTest {

    private static int testPort = -1;
    private static LokiHttpServerMock mockLoki;

    @BeforeClass
    public static void startMockLoki() {
        testPort = 20_000 + new Random().nextInt(10_000);
        mockLoki = lokiMock(testPort);
        mockLoki.start();
    }

    @AfterClass
    public static void stopMockLoki() {
        mockLoki.stop();
    }

    @Before
    public void resetMockLoki() {
        mockLoki.reset();
    }

    private static LokiApacheHttpAppender apacheHttpAppender(int port, int batchSize, long batchTimeoutMs) {
        var appender = new LokiApacheHttpAppender();

        appender.setUrl(String.format("http://localhost:%s/loki/api/v1/push", port));
        appender.setConnectionTimeoutMs(1000L);
        appender.setRequestTimeoutMs(500L);

        appender.setContext(new LoggerContext());
        appender.setBatchSize(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        appender.setEncoder(defaultToStringEncoder());
        appender.setVerbose(true);
        appender.setProcessingThreads(1);
        appender.setHttpThreads(1);

        return appender;
    }

    @Test
    public void testHttpSend() {
        withAppender(apacheHttpAppender(testPort, 3, 1000L), a -> {
            a.appendAndWait(events[0], events[1]);
            assertTrue("no batches before batchSize reached", mockLoki.lastBatch == null);

            a.appendAndWait(events[2]);
            assertEquals("http send", expected, new String(mockLoki.lastBatch));
        });
    }

}
