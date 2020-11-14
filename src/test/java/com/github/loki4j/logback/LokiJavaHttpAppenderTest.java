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

public class LokiJavaHttpAppenderTest {

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

    private static LokiJavaHttpAppender javaHttpAppender(int port, int batchSize, long batchTimeoutMs) {
        var appender = new LokiJavaHttpAppender();

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
        withAppender(javaHttpAppender(testPort, 3, 1000L), appender -> {
            appender.append(events[0]);
            appender.append(events[1]);
            assertTrue("no batches before batchSize reached", mockLoki.lastBatch == null);

            appender.append(events[2]);
            try { Thread.sleep(500L); } catch (InterruptedException e1) { }
            assertEquals("http send", expected, new String(mockLoki.lastBatch));
        });
    }
    
}
