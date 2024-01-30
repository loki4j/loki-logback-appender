package com.github.loki4j.testkit.dummy;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class StringPayload {

    public static final String LABELS_MESSAGE_SEPARATOR = " %%% ";

    public final HashMap<String, ArrayList<String>> data;

    private StringPayload(HashMap<String, ArrayList<String>> data) {
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
        HashMap<String, ArrayList<String>> data = new HashMap<>();
        var lines = input.split("\n");
        for (String line : lines) {
            var pair = line.split(LABELS_MESSAGE_SEPARATOR);
            var recs = data.computeIfAbsent(pair[0], x -> new ArrayList<>());
            recs.add(pair[1]);
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
        private HashMap<String, ArrayList<String>> data = new HashMap<>();
        public StringPayloadBuilder stream(String stream, String... records) {
            var recs = data.computeIfAbsent(stream, x -> new ArrayList<>());
            recs.addAll(Arrays.asList(records));
            return this;
        }
        public StringPayload build() {
            return new StringPayload(data);
        }
    }
}
