package com.github.loki4j.common;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class LogRecordStream {

    private static AtomicLong labelSetIds = new AtomicLong(0L);

    public final long id;

    public final String[] labels;

    public final int utf8SizeBytes;

    private LogRecordStream(String[] labels) {
        this.id = labelSetIds.getAndIncrement();
        this.labels = labels;

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
        result = prime * result + (int) (id ^ (id >>> 32));
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
        if (id != other.id)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Stream [id=" + id + ", labels=" + Arrays.toString(labels) + "]";
    }

}
