package com.github.loki4j.client.batch;

import java.util.Map;

import com.github.loki4j.client.util.StringUtils;

public class LogRecord {

    public final long timestampMs;

    public final int nanosInMs;

    public final Map<String, String> stream;

    public final int streamUtf8SizeBytes;

    public final String message;

    public final int messageUtf8SizeBytes;

    public final Map<String, String> metadata;

    public final int metadataUtf8SizeBytes;

    private LogRecord(
            long timestampMs,
            int nanosInMs,
            Map<String, String> stream,
            String message,
            Map<String, String> metadata) {
        this.timestampMs = timestampMs;
        this.nanosInMs = nanosInMs;

        this.message = message;
        this.messageUtf8SizeBytes = StringUtils.utf8Length(message);

        this.stream = stream;
        this.streamUtf8SizeBytes = kvpUtf8SizeBytes(stream);

        this.metadata = metadata;
        this.metadataUtf8SizeBytes = kvpUtf8SizeBytes(metadata);
    }

    public static LogRecord create(
            long timestampMs,
            int nanosInMs,
            Map<String, String> stream,
            String message,
            Map<String, String> metadata) {
        return new LogRecord(timestampMs, nanosInMs, stream, message, metadata);
    }

    @Override
    public String toString() {
        return "LogRecord [ts=" + timestampMs
                + ", nanos=" + nanosInMs
                + ", stream=" + stream
                + ", message=" + message
                + ", metadata=" + metadata
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (timestampMs ^ (timestampMs >>> 32));
        result = prime * result + nanosInMs;
        result = prime * result + ((stream == null) ? 0 : stream.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
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
        LogRecord other = (LogRecord) obj;
        if (timestampMs != other.timestampMs)
            return false;
        if (nanosInMs != other.nanosInMs)
            return false;
        if (stream == null) {
            if (other.stream != null)
                return false;
        } else if (!stream.equals(other.stream))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        return true;
    }

    private static int kvpUtf8SizeBytes(Map<String, String> map) {
        var utf8SizeBytes = 0;
        for (var entry : map.entrySet()) {
            utf8SizeBytes += StringUtils.utf8Length(entry.getKey());
            utf8SizeBytes += StringUtils.utf8Length(entry.getValue());
        }
        return utf8SizeBytes;
    }

}
