package com.github.loki4j.logback.integration;

import static com.github.loki4j.logback.Generators.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.loki4j.common.LogRecord;
import com.github.loki4j.logback.Loki4jAppender;
import com.github.loki4j.logback.AbstractLoki4jEncoder;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LokiTestingClient {

    private String urlQuery;

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ObjectMapper json = new ObjectMapper();

    public LokiTestingClient(String urlBase) {
        urlQuery = urlBase + "/query";

        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(120))
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofSeconds(30));
    }

    public LokiTestingClient(String urlBase, String username, String password) {
        this(urlBase);

        requestBuilder.setHeader("Authorization", "Basic " +
            Base64
                .getEncoder()
                .encodeToString((username + ":" + password).getBytes())
        );
    }

    public void close() {

    }

    public String queryRecords(String testLabel, int limit, String time) {

        try {
            var query = URLEncoder.encode("{test=\"" + testLabel + "\"}", "utf-8");
            var url = URI.create(String.format(
                "%s?query=%s&limit=%s&time=%s&direction=forward", urlQuery, query, limit, time));
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

    public LokiRequest parseRequest(String req) throws JsonMappingException, JsonProcessingException {
        return json.readValue(req, LokiRequest.class);
    }

    public LokiResponse parseResponse(String res) throws JsonMappingException, JsonProcessingException {
        return json.readValue(res, LokiResponse.class);
    }

    public void testHttpSend(
            String lbl,
            ILoggingEvent[] events,
            Loki4jAppender actualAppender,
            AbstractLoki4jEncoder expectedEncoder) throws Exception {
        testHttpSend(lbl, events, actualAppender, expectedEncoder, events.length, 5000L);
    }

    public void testHttpSend(
            String lbl,
            ILoggingEvent[] events,
            Loki4jAppender actualAppender,
            AbstractLoki4jEncoder expectedEncoder,
            int chunkSize,
            long chunkDelayMs) throws Exception {
        var records = new LogRecord[events.length];
        var reqStr = new AtomicReference<String>();

        withAppender(actualAppender, a -> {
            for (int i = 0; i < events.length; i += chunkSize) {
                var chunk = Arrays.copyOfRange(events, i, Math.min(events.length, i + chunkSize));
                //System.out.println(String.format("%s: %s + %s", Thread.currentThread().getName(), i, chunk.length));
                a.appendAndWait(chunk);
                if (chunkDelayMs > 0L)
                    try { Thread.sleep(chunkDelayMs); } catch (InterruptedException e) { }
            }
            return null;
        });
        withEncoder(expectedEncoder, encoder -> {
            for (int i = 0; i < events.length; i++) {
                records[i] = new LogRecord();
                encoder.eventToRecord(events[i], records[i]);
            }
            reqStr.set(new String(encoder.encode(records)));
        });

        var req = parseRequest(reqStr.get());
        var lastIdx = records.length - 1;
        var time = String.format("%s%06d", records[lastIdx].timestampMs + 100, records[lastIdx].nanos);
        var resp = parseResponse(queryRecords(lbl, events.length, time));
        //System.out.println(req + "\n\n");
        //System.out.println(resp);
        assertEquals(lbl + " status", "success", resp.status);
        assertEquals(lbl + " result type", "streams", resp.data.resultType);
        assertEquals(lbl + " stream count", req.streams.size(), resp.data.result.size());
        assertEquals(lbl + " event count",
            req.streams.stream().mapToInt(s -> s.values.size()).sum(),
            resp.data.result.stream().mapToInt(s -> s.values.size()).sum());
        assertEquals(lbl + " content", req.streams, resp.data.result);
    }

    public static class Stream {
        public Map<String, String> stream;
        public List<List<String>> values;
        @Override
        public String toString() {
            return String.format("Stream [stream=\n%s,values=\n\t%s]",
                stream,
                String.join("\n\t", values.stream().map(x -> x.toString()).collect(Collectors.toList())));
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((stream == null) ? 0 : stream.hashCode());
            result = prime * result + ((values == null) ? 0 : values.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Stream other = (Stream) obj;
            if (stream == null) {
                if (other.stream != null)
                    return false;
            } else if (!stream.equals(other.stream))
                return false;
            if (values == null) {
                if (other.values != null)
                    return false;
            } else if (!values.equals(other.values))
                return false;
            return true;
        }
    }
    public static class LokiRequest {
        public List<Stream> streams;
        @Override
        public String toString() {
            return String.format("LokiRequest [streams=%s]", streams);
        }
    }
    public static class LokiResponse {
        @JsonIgnoreProperties({ "stats" })
        public static class ResponseData {
            public String resultType;
            public List<Stream> result;
        }
        public String status;
        public ResponseData data;
        @Override
        public String toString() {
            return String.format("LokiResponse [status=%s,data=%s]", status, data.result);
        }
    }
}
