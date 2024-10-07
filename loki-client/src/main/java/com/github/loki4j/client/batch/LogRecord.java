package com.github.loki4j.client.batch;

import java.util.Arrays;

import com.github.loki4j.client.util.StringUtils;

public class LogRecord {

    public final long timestampMs;

    public final int nanosInMs;

    public final LogRecordStream stream;

    public final String message;

    public final int messageUtf8SizeBytes;

    public final String[] metadata;

    public final int metadataUtf8SizeBytes;

    private LogRecord(
            long timestampMs,
            int nanosInMs,
            LogRecordStream stream,
            String message,
            String[] metadata) {
        this.timestampMs = timestampMs;
        this.nanosInMs = nanosInMs;
        this.stream = stream;
        this.message = message;
        this.messageUtf8SizeBytes = StringUtils.utf8Length(message);

        this.metadata = metadata;
        var metadataUtf8SizeBytes = 0;
        for (int i = 0; i < metadata.length; i++) {
            metadataUtf8SizeBytes += StringUtils.utf8Length(metadata[i]);
        }
        this.metadataUtf8SizeBytes = metadataUtf8SizeBytes;
    }

    public static LogRecord create(
            long timestampMs,
            int nanosInMs,
            LogRecordStream stream,
            String message,
            String[] metadata) {
        return new LogRecord(timestampMs, nanosInMs, stream, message, metadata);
    }

    @Override
    public String toString() {
        return "LogRecord [ts=" + timestampMs
                + ", nanos=" + nanosInMs
                + ", stream=" + stream
                + ", message=" + message
                + ", metadata=" + Arrays.toString(metadata)
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
        } else if (!Arrays.equals(metadata, other.metadata))
            return false;
        return true;
    }

}
