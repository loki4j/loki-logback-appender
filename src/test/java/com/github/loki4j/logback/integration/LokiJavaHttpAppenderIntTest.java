package com.github.loki4j.logback.integration;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.AbstractLoki4jAppender;
import com.github.loki4j.logback.JsonEncoder;
import com.github.loki4j.logback.Loki4jEncoder;
import com.github.loki4j.logback.LokiApacheHttpAppender;
import com.github.loki4j.logback.LokiJavaHttpAppender;
import com.github.loki4j.logback.integration.LokiStructures.LokiRequest;
import com.github.loki4j.logback.integration.LokiStructures.LokiResponse;

import static com.github.loki4j.logback.Generators.*;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

import ch.qos.logback.classic.LoggerContext;

public class LokiJavaHttpAppenderIntTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";
    private static String urlQuery = urlBase + "/query";
    private static HttpClient client;
    private static HttpRequest.Builder requestBuilder;

    @BeforeClass
    public static void startMockLoki() {
        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(5_000))
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(1_000))
            .uri(URI.create(urlQuery));
    }

    @AfterClass
    public static void stopMockLoki() {
        //mockLoki.stop();
    }

    @Before
    public void resetMockLoki() {
        //mockLoki.reset();
    }

    public static String queryRecords(String testLabel, int limit) {
        try {
            var query = URLEncoder.encode("{test=\"" + testLabel + "\"}", "utf-8");
            var url = URI.create(String.format(
                "%s?query=%s&limit=%s&direction=forward", urlQuery, query, limit));
            //System.out.println(url);
            var req = requestBuilder.copy()
                .uri(url)
                .GET()
                .build();
        
            return 
                client
                    .send(req, HttpResponse.BodyHandlers.ofString())
                    .body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static AbstractLoki4jAppender javaHttpAppender(int batchSize, long batchTimeoutMs, Loki4jEncoder encoder) {
        var appender = new LokiApacheHttpAppender(); //LokiJavaHttpAppender();

        appender.setUrl(urlPush);
        appender.setConnectionTimeoutMs(1000L);
        appender.setRequestTimeoutMs(500L);

        appender.setContext(new LoggerContext());
        appender.setBatchSize(batchSize);
        appender.setBatchTimeoutMs(batchTimeoutMs);
        appender.setEncoder(encoder);
        appender.setVerbose(true);
        appender.setProcessingThreads(1);
        appender.setHttpThreads(1);

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
    public void testHttpSend() throws Exception {
        var lbl = "httpSend";
        var eventCount = 1000;
        var events = generateEvents(eventCount, 10);
        var records = new LogRecord[events.length];
        var reqStr = new AtomicReference<String>();

        var mapper = new ObjectMapper();
        withAppender(javaHttpAppender(10, 1000L, jsonEncoder(false, lbl)), appender -> {
            for (int i = 0; i < events.length; i++) {
                appender.doAppend(events[i]);
                //try { Thread.sleep(5L); } catch (InterruptedException e1) { }
            }
            try { Thread.sleep(500L); } catch (InterruptedException e1) { }
        });
        withEncoder(jsonEncoder(false, lbl), encoder -> {
            for (int i = 0; i < events.length; i++) {
                records[i] = new LogRecord();
                encoder.eventToRecord(events[i], records[i]);
            }
            reqStr.set(new String(encoder.encode(records)));
        });

        var req = mapper.readValue(reqStr.get(), LokiRequest.class);
        var resp = mapper.readValue(queryRecords(lbl, eventCount), LokiResponse.class);
        //System.out.println(req + "\n\n");
        //System.out.println(resp);
        assertEquals("http send", "success", resp.status);
        assertEquals("http send", "streams", resp.data.resultType);
        assertEquals("http send size", req.streams.size(), resp.data.result.size());
        assertEquals("http send content", req.streams, resp.data.result);
    }
    
}
