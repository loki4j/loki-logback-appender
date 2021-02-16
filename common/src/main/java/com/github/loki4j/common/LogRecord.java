package com.github.loki4j.common;

public class LogRecord {

    public long timestampMs;

    public int nanos;

    public String stream;

    public String message;

    public static LogRecord create() {
        return new LogRecord();
    }

    public static LogRecord create(
            long timestamp,
            int nanos,
            String stream,
            String message) {
        var r = new LogRecord();
        r.timestampMs = timestamp;
        r.nanos = nanos;
        r.stream = stream;
        r.message = message;
        return r;
    }

    @Override
    public String toString() {
        return "LogRecord [ts=" + timestampMs +
            ", nanos=" + nanos +
            ", stream=" + stream +
            ", message=" + message + "]";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + nanos;
		result = prime * result + ((stream == null) ? 0 : stream.hashCode());
		result = prime * result + (int) (timestampMs ^ (timestampMs >>> 32));
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
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (nanos != other.nanos)
			return false;
		if (stream == null) {
			if (other.stream != null)
				return false;
		} else if (!stream.equals(other.stream))
			return false;
		if (timestampMs != other.timestampMs)
			return false;
		return true;
	}

    
    
}
