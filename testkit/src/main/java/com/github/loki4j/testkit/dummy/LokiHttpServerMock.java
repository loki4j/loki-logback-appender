package com.github.loki4j.testkit.dummy;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LokiHttpServerMock {
    public List<byte[]> batches = new ArrayList<>();
    public volatile byte[] lastBatch;
    public volatile Map<String, List<String>> lastHeaders;

    private final HttpServer server;

    public LokiHttpServerMock(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/loki/api/v1/push", httpExchange -> {
            lastHeaders = httpExchange.getRequestHeaders();
            try (var is = httpExchange.getRequestBody()) {
                lastBatch = getBytesFromInputStream(is); //is.readAllBytes();
                batches.add(lastBatch);
            }
            httpExchange.sendResponseHeaders(204, -1);
        });
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    public void start() {
        new Thread(server::start).start();
    }

    public void stop() {
        server.stop(0);
    }

    public void reset() {
        batches.clear();
        lastBatch = null;
    }
}
