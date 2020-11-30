package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.github.loki4j.common.LogRecord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParSendTest {

    private static String urlBase = "http://localhost:3100/loki/api/v1";
    private static String urlPush = urlBase + "/push";

    private static LokiTestingClient client;

    @BeforeClass
    public static void startMockLoki() {
        client = new LokiTestingClient(urlBase);
    }

    @AfterClass
    public static void stopMockLoki() {
        client.close();
    }

    @Test
    @Category({IntegrationTests.class})
    public void testJavaJsonParSend() throws Exception {
        var events = generateEvents(500, 500);

        var parFactor = 8;
        ExecutorService tp = Executors.newFixedThreadPool(parFactor);
        var fs = new CompletableFuture[parFactor];
        for (int i = 0; i < parFactor; i++) {
            var idx = i;
            var label = "testJavaJsonParSend" + idx;
            var appender = javaHttpAppender(urlPush);
            appender.setBatchSize(10);
            appender.setBatchTimeoutMs(150_000);
            appender.setEncoder(jsonEncoder(false, label));

            fs[i] = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        client.testHttpSend(
                            label,
                            events,
                            appender,
                            jsonEncoder(false, label),
                            5,
                            (parFactor - idx) + 500L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return "label";
                }, tp);
        }
        for (int i = 0; i < fs.length; i++) {
            assertEquals("label", fs[i].get());
        }
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonParSend() throws Exception {
        var events = generateEvents(500, 500);

        var parFactor = 8;
        ExecutorService tp = Executors.newFixedThreadPool(parFactor);
        var fs = new CompletableFuture[parFactor];
        for (int i = 0; i < parFactor; i++) {
            var idx = i;
            var label = "testApacheJsonParSend" + idx;
            var appender = apacheHttpAppender(urlPush);
            appender.setBatchSize(10);
            appender.setBatchTimeoutMs(150_000);
            appender.setEncoder(jsonEncoder(false, label));

            fs[i] = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        client.testHttpSend(
                            label,
                            events,
                            appender,
                            jsonEncoder(false, label),
                            5,
                            (parFactor - idx) + 500L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return "label";
                }, tp);
        }
        for (int i = 0; i < fs.length; i++) {
            assertEquals("label", fs[i].get());
        }
    }

    @Test
    @Category({IntegrationTests.class})
    public void testApacheJsonSend() throws Exception {
        var events = generateEvents(3, 20);

        var label = "testApacheJsonSend";
        var appender = apacheHttpAppender(urlPush);
        appender.setBatchSize(2);
        appender.setBatchTimeoutMs(150_000);
        appender.setEncoder(jsonEncoder(false, label));
        
        client.testHttpSend(label, events, appender, jsonEncoder(false, label));
    }

    @Test
    @Category({IntegrationTests.class})
    public void testRawPersistentConns() throws Exception {
        var parFactor = 8;
        var numBatches = 1;
        ExecutorService tp = Executors.newFixedThreadPool(parFactor);
        var fs = new CompletableFuture[parFactor];
        for (int i = 0; i < parFactor; i++) {
            var idx = i;
            var label = "testRawPersistentConns" + idx;

            fs[i] = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        var client = new DummyClient();
                        client.startConnection("localhost", 3100);

                        for (int j = 0; j < numBatches; j++) {
                            var events = generateEvents(3, 20);
                            AtomicReference<byte[]> body = new AtomicReference<>();
                            withEncoder(jsonEncoder(false, label), encoder -> {
                                var records = new LogRecord[events.length];
                                for (int k = 0; k < events.length; k++) {
                                    records[k] = encoder.eventToRecord(events[k], new LogRecord());
                                }
                                body.set(encoder.encode(records));
                            });
                            var batch = new StringBuilder();
                            batch
                                .append("POST /loki/api/v1/push HTTP/1.1\n")
                                .append("Host: localhost:3100\n")
                                .append("Connection: Keep-Alive\n")
                                .append("Keep-Alive: timeout=5, max=1000\n")
                                .append("Content-type: application/json\n")
                                .append("Content-Length: " + body.get().length + "\n")
                                .append('\n');

                            //System.out.println(batch.toString());
                            var response = client.sendMessage(batch.toString(), body.get());
                            //System.out.println(">>> " + idx + ": " + response);
                            assertTrue("Raw HTTP", response.startsWith("HTTP/1.1 204"));
                            //Thread.sleep((idx + 1) * 10);
                        }

                        client.stopConnection();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return "label";
                }, tp);
        }
        for (int i = 0; i < fs.length; i++) {
            assertEquals("label", fs[i].get());
        }
    }

    public static class DummyClient {
	    private Socket clientSocket;
	    private OutputStream out;
	    private BufferedReader in;
	 
	    public void startConnection(String host, int port) throws Exception {
            clientSocket = new Socket(host, port);
            //clientSocket.setTcpNoDelay(true);
            //clientSocket.setSoTimeout(30_000);
	        out = clientSocket.getOutputStream();
	        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	    }
	 
	    public String sendMessage(String headers, byte[] body) throws IOException {
            out.write(headers.getBytes("utf-8"));
            out.write(body);
            var sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }
	        return sb.toString();
	    }
	 
	    public void stopConnection() throws IOException {
	        in.close();
	        out.close();
	        clientSocket.close();
	    }
	}

}
