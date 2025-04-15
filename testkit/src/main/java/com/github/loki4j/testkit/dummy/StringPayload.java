package com.github.loki4j.testkit.dummy;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StringPayload {

    public static final String LABELS_MESSAGE_SEPARATOR = " %%% ";

    public final HashMap<String, ArrayList<StringLogRecord>> data;

    private StringPayload(HashMap<String, ArrayList<StringLogRecord>> data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
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
        StringPayload other = (StringPayload) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "StringPayload [" + data + "]";
    }

    public static StringPayload parse(String input) {
        HashMap<String, ArrayList<StringLogRecord>> data = new HashMap<>();
        var lines = input.split("\n");
        for (String line : lines) {
            var pair = line.split(LABELS_MESSAGE_SEPARATOR);
            var recs = data.computeIfAbsent(pair[0], x -> new ArrayList<>());
            recs.add(new StringLogRecord(pair[2], pair[1]));
        }
        return new StringPayload(data);
    }

    public static StringPayload parse(byte[] input) {
        return parse(new String(input));
    }

    public static StringPayload parse(byte[] input, Charset charset) {
        return parse(new String(input, charset));
    }

    public static StringPayloadBuilder builder() {
        return new StringPayloadBuilder();
    }

    public static class StringPayloadBuilder {
        private HashMap<String, ArrayList<StringLogRecord>> data = new HashMap<>();
        public StringPayloadBuilder stream(Map<String, String> stream, String... records) {
            var recs = data.computeIfAbsent(stream.toString(), x -> new ArrayList<>());
            recs.addAll(Arrays.asList(records).stream()
                    .map(r -> StringLogRecord.of(r, Map.of()))
                    .collect(Collectors.toList())
            );
            return this;
        }
        public StringPayloadBuilder streamWithMeta(Map<String, String> stream, StringLogRecord... records) {
            var recs = data.computeIfAbsent(stream.toString(), x -> new ArrayList<>());
            recs.addAll(Arrays.asList(records));
            return this;
        }
        public StringPayload build() {
            return new StringPayload(data);
        }
    }

    public static class StringLogRecord {
        private String metadata;
        private String line;
        public StringLogRecord(String line, String metadata) {
            this.metadata = metadata;
            this.line = line;
        }
        public static StringLogRecord of(String line, Map<String, String> metadataKvp) {
            return new StringLogRecord(line, metadataKvp.toString());
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
            result = prime * result + ((line == null) ? 0 : line.hashCode());
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
            StringLogRecord other = (StringLogRecord) obj;
            if (metadata == null) {
                if (other.metadata != null)
                    return false;
            } else if (!metadata.equals(other.metadata))
                return false;
            if (line == null) {
                if (other.line != null)
                    return false;
            } else if (!line.equals(other.line))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "LogRecord [metadata=" + metadata + ", line=" + line + "]";
        }
    }
}
