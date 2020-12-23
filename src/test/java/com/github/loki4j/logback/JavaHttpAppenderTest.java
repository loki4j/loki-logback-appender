package com.github.loki4j.logback;

import java.util.Random;

import com.github.loki4j.logback.Generators.LokiHttpServerMock;
import static com.github.loki4j.logback.Generators.*;
import static com.github.loki4j.logback.Loki4jAppenderTest.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

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
            a.appendAndWait(events[0], events[1]);
            assertTrue("no batches before batchSize reached", mockLoki.lastBatch == null);

            a.appendAndWait(events[2]);
            assertEquals("http send", expected, new String(mockLoki.lastBatch));

            return null;
        });
    }

    @Test
    public void testDefaults() {
        var appender = new Loki4jAppender();
        appender.setBatchSize(3);
        appender.getSender().setUrl(url);
        appender.setVerbose(true);
        appender.setContext(new LoggerContext());

        var expected = (
            "{'streams':[{'stream':{'host':'${HOSTNAME}','level':'INFO'}," +
            "'values':[['100000001','l=INFO c=test.TestApp t=thread-1 | Test message 1 ']," +
            "['107000003','l=INFO c=test.TestApp t=thread-1 | Test message 3 ']]}," +
            "{'stream':{'host':'${HOSTNAME}','level':'WARN'}," +
            "'values':[['104000002','l=WARN c=test.TestApp t=thread-2 | Test message 2 ']]}]}"
            ).replace('\'', '"');
        
        withAppender(appender, a -> {
            a.appendAndWait(events);
            assertEquals("http send", expected, new String(mockLoki.lastBatch));
            return null;
        });
    }

}
