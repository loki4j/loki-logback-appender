package com.github.loki4j.logback.integration;

import static java.util.stream.Collectors.toSet;
import static com.github.loki4j.logback.Generators.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.loki4j.client.batch.LogRecord;
import com.github.loki4j.client.batch.LogRecordBatch;
import com.github.loki4j.client.http.HttpHeader;
import com.github.loki4j.client.pipeline.PipelineConfig;
import com.github.loki4j.client.util.ByteBufferFactory;
import com.github.loki4j.logback.Loki4jAppender;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class LokiTestingClient {

    private static Comparator<LogRecord> byStream = (e1, e2) -> String.CASE_INSENSITIVE_ORDER.compare(
        e1.stream.toString(), e2.stream.toString());
    private static Comparator<LogRecord> byTime = (e1, e2) ->
        Long.compare(e1.timestampMs, e2.timestampMs);
    private static Comparator<LogRecord> lokiLogsSorting = byStream.thenComparing(byTime);

    private String urlQuery;

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ObjectMapper json = new ObjectMapper();

    public LokiTestingClient(String urlBase) {
        urlQuery = urlBase + "/query_range";

        client = HttpClient
            .newBuilder()
            .version(Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(120))
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofSeconds(30));
    }

    public LokiTestingClient(String urlBase, String tenant){
        this(urlBase);
        requestBuilder.setHeader(HttpHeader.X_SCOPE_ORGID, tenant);
    }

    public LokiTestingClient(String urlBase, String username, String password) {
        this(urlBase);

        requestBuilder.setHeader(HttpHeader.AUTHORIZATION, "Basic " +
            Base64
                .getEncoder()
                .encodeToString((username + ":" + password).getBytes())
        );
    }

    public void close() {

    }

    public String queryRecords(String testLabel, int limit, String time) {
        try {
            var query = URLEncoder.encode("{test=\"" + testLabel + "\"} | drop detected_level", "utf-8");
            var url = URI.create(String.format(
                "%s?query=%s&limit=%s&end=%s&direction=forward", urlQuery, query, limit, time));
            //System.err.println(url);
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
            Loki4jAppender actualAppender) throws Exception {
        var chunkSize = events.length;
        var chunkDelayMs = 10L;
        testHttpSend(lbl, events, actualAppender, null, chunkSize, chunkDelayMs);
    }

    public void testHttpSend(
            String lbl,
            ILoggingEvent[] events,
            Loki4jAppender actualAppender,
            Loki4jAppender expectedAppender,    // if null - the default json appender is used
            int chunkSize,
            long chunkDelayMs) throws Exception {
        var records = new LogRecord[events.length];
        var reqStr = new AtomicReference<String>();

        withAppender(actualAppender, a -> {
            for (int i = 0; i < events.length; i += chunkSize) {
                var chunk = Arrays.copyOfRange(events, i, Math.min(events.length, i + chunkSize));
                //System.out.println(String.format("%s: %s + %s", Thread.currentThread().getName(), i, chunk.length));
                a.append(chunk);
                if (chunkDelayMs > 0L)
                    try { Thread.sleep(chunkDelayMs); } catch (InterruptedException e) { }
            }
            a.waitAllAppended();
            return null;
        });
        // forming expected output
        if (expectedAppender == null)
            expectedAppender = appender(lbl, batch(chunkSize, chunkDelayMs), http(null));
        withAppender(expectedAppender, encoder -> {
            for (int i = 0; i < events.length; i++) {
                final var idx = i;
                records[i] = encoder.eventToLogRecord(events[idx]);
            }
            var batch = new LogRecordBatch(records);
            batch.sort(lokiLogsSorting);
            var writer = PipelineConfig.json.factory.apply(4 * 1024 * 1024, new ByteBufferFactory(false));
            writer.serializeBatch(batch);
            reqStr.set(new String(writer.toByteArray()));
            return null;
        });

        var req = parseRequest(reqStr.get());
        var lastIdx = records.length - 1;
        var time = String.format("%s%06d", records[lastIdx].timestampMs + 100, 0);
        var resp = parseResponse(queryRecords(lbl, events.length, time));
        //System.out.println(req + "\n\n");
        //System.out.println(resp);
        assertEquals("success", resp.status, lbl + " status");
        assertEquals("streams", resp.data.resultType, lbl + " result type");
        assertEquals(
                req.streams.stream().map(s -> s.stream).collect(toSet()),
                resp.data.result.stream().map(s -> s.stream).collect(toSet()),
                lbl + " stream");
        assertEquals(
                req.streams.stream().mapToInt(s -> s.values.size()).sum(),
                resp.data.result.stream().mapToInt(s -> s.values.size()).sum(),
                lbl + " event count");
        assertEquals(req.streams, resp.data.result, lbl + " content");
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
