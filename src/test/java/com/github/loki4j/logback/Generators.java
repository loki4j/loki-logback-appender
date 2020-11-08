package com.github.loki4j.logback;

import com.github.loki4j.common.LogRecord;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class Generators {

    public static Exception exception(String message) {
        return new RuntimeException(message);
    }

    public static String batchToString(LogRecord[] batch) {
        var s = new StringBuilder();
        for (int i = 0; i < batch.length; i++) {
            s.append(batch[i]);
            s.append('\n');
        }
        return s.toString();
    }

    public static AbstractLoki4jEncoder toStringEncoder(
        AbstractLoki4jEncoder.LabelCfg label,
        AbstractLoki4jEncoder.MessageCfg message,
        boolean sortByTime,
        boolean staticLabels) {
        var encoder = new AbstractLoki4jEncoder() {
            @Override
            public String getContentType() {
                return "text/plain";
            }
            @Override
            protected byte[] encodeStaticLabels(LogRecord[] batch) {
                return batchToString(batch).getBytes(charset);
            }
            @Override
            protected byte[] encodeDynamicLabels(LogRecord[] batch) {
                return batchToString(batch).getBytes(charset);
            }
        };
        encoder.setContext(new LoggerContext());
        encoder.setLabel(label);
        encoder.setMessage(message);
        encoder.setSortByTime(sortByTime);
        encoder.setStaticLabels(staticLabels);
        return encoder;
    }

    public static AbstractLoki4jEncoder.LabelCfg labelCfg(
            String pattern,
            String pairSeparator,
            String keyValueSeparator,
            boolean nopex) {
        var label = new AbstractLoki4jEncoder.LabelCfg();
        label.setPattern(pattern);
        label.setPairSeparator(pairSeparator);
        label.setKeyValueSeparator(keyValueSeparator);
        label.setNopex(nopex);
        return label;
    }

    public static AbstractLoki4jEncoder.MessageCfg messageCfg(
            String pattern) {
        var message = new AbstractLoki4jEncoder.MessageCfg();
        message.setPattern(pattern);
        return message;
    }

    public static ILoggingEvent loggingEvent(
            long timestamp,
            Level level,
            String className,
            String threadName,
            String message,
            Throwable throwable) {
        var e = new LoggingEvent();
        e.setTimeStamp(timestamp);
        e.setLevel(level);
        e.setLoggerName(className);
        e.setThreadName(threadName);
        e.setMessage(message);
        if (throwable != null)
            e.setThrowableProxy(new ThrowableProxy(throwable));
        return e;
    }

    public static LogRecord logRecord(
            long timestamp,
            int nanos,
            String stream,
            String message) {
        var r = new LogRecord();
        r.timestampMs = timestamp;
        r.nanos = nanos;
        r.stream = stream;
        r.streamHashCode = stream.hashCode();
        r.message = message;
        return r;
    }
    
}
