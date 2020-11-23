package com.github.loki4j.logback.integration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class LokiStructures {
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
