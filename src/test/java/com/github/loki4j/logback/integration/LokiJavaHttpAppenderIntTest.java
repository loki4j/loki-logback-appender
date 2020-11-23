package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.AbstractLoki4jAppender;
import com.github.loki4j.logback.AbstractLoki4jEncoder;
import com.github.loki4j.logback.JsonEncoder;
import com.github.loki4j.logback.LokiApacheHttpAppender;
import com.github.loki4j.logback.LokiJavaHttpAppender;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import ch.qos.logback.classic.LoggerContext;

public class LokiJavaHttpAppenderIntTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";

    private static LokiClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiClient(urlBase);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }

    private static LokiJavaHttpAppender javaHttpAppender() {
        var appender = new LokiJavaHttpAppender();

        appender.setUrl(urlPush);
        appender.setConnectionTimeoutMs(1000L);
        appender.setRequestTimeoutMs(500L);
        appender.setContext(new LoggerContext());
        appender.setVerbose(true);

        return appender;
    }

    private static LokiApacheHttpAppender apacheHttpAppender() {
        var appender = new LokiApacheHttpAppender();

        appender.setUrl(urlPush);
        appender.setConnectionTimeoutMs(1000L);
        appender.setRequestTimeoutMs(500L);
        appender.setContext(new LoggerContext());
        appender.setVerbose(true);

        return appender;
    }

    private static JsonEncoder jsonEncoder(boolean staticLabels, String testLabel) {
        var encoder = new JsonEncoder();
        encoder.setStaticLabels(staticLabels);
        encoder.setLabel(labelCfg("test=" + testLabel + ",level=%level,app=my-app", ",", "=", true));
        encoder.setMessage(messageCfg("l=%level c=%logger{20} t=%thread | %msg %ex{1}"));
        encoder.setSortByTime(true);
        return encoder;
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonFastSend() throws Exception {
        var label = "testApacheJsonFastSend";
        var appender = apacheHttpAppender();
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(jsonEncoder(false, label));

        testHttpSend(label, 1000, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonFastSend() throws Exception {
        var label = "testJavaJsonFastSend";
        var appender = javaHttpAppender();
        appender.setBatchSize(10);
        appender.setBatchTimeoutMs(1000);
        appender.setEncoder(jsonEncoder(false, label));

        testHttpSend(label, 1000, appender, jsonEncoder(false, label));
    }

    private void testHttpSend(
            String lbl,
            int eventCount,
            AbstractLoki4jAppender actualAppender,
            AbstractLoki4jEncoder expectedEncoder) throws Exception {
        var events = generateEvents(eventCount, 10);
        var records = new LogRecord[events.length];
        var reqStr = new AtomicReference<String>();

        withAppender(actualAppender, appender -> {
            for (int i = 0; i < events.length; i++) {
                appender.doAppend(events[i]);
                //try { Thread.sleep(5L); } catch (InterruptedException e1) { }
            }
            try { Thread.sleep(500L); } catch (InterruptedException e1) { }
        });
        withEncoder(expectedEncoder, encoder -> {
            for (int i = 0; i < events.length; i++) {
                records[i] = new LogRecord();
                encoder.eventToRecord(events[i], records[i]);
            }
            reqStr.set(new String(encoder.encode(records)));
        });

        var req = client.parseRequest(reqStr.get());
        var resp = client.parseResponse(client.queryRecords(lbl, eventCount));
        //System.out.println(req + "\n\n");
        //System.out.println(resp);
        assertEquals(lbl + " status", "success", resp.status);
        assertEquals(lbl + " result type", "streams", resp.data.resultType);
        assertEquals(lbl + " event count", req.streams.size(), resp.data.result.size());
        assertEquals(lbl + " content", req.streams, resp.data.result);
    }

}
