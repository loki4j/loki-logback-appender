package com.github.loki4j.logback.integration;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LokiClient {

    private String urlQuery;

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    private ObjectMapper json = new ObjectMapper();

    public LokiClient(String urlBase) {
        urlQuery = urlBase + "/query";

        client = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofMillis(5_000))
            .build();

        requestBuilder = HttpRequest
            .newBuilder()
            .timeout(Duration.ofMillis(1_000));
    }

    public void close() {

    }

    public String queryRecords(String testLabel, int limit) {

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

    public LokiRequest parseRequest(String req) throws JsonMappingException, JsonProcessingException {
        return json.readValue(req, LokiRequest.class);
    }

    public LokiResponse parseResponse(String res) throws JsonMappingException, JsonProcessingException {
        return json.readValue(res, LokiResponse.class);
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
