package com.github.loki4j.client.batch;

import java.util.Arrays;

import com.github.loki4j.client.util.StringUtils;

public class LogRecordStream {

    public final String[] labels;

    public final int hash;

    public final int utf8SizeBytes;

    private LogRecordStream(String[] labels) {
        this.labels = labels;
        this.hash = Arrays.hashCode(labels);

        var sizeBytes = 0;
        for (int i = 0; i < labels.length; i++) {
            sizeBytes += StringUtils.utf8Length(labels[i]);
        }
        utf8SizeBytes = sizeBytes;
    }

    public static LogRecordStream create(String... labels) {
        return new LogRecordStream(labels);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (hash ^ (hash >>> 32));
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
        LogRecordStream other = (LogRecordStream) obj;
        if (!Arrays.equals(labels, other.labels))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Stream [hash=" + hash + ", labels=" + Arrays.toString(labels) + "]";
    }
}
