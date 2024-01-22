package com.github.loki4j.logback;

import java.util.List;
import java.util.function.Supplier;

import com.github.loki4j.logback.json.JsonEventWriter;
import com.github.loki4j.logback.json.JsonProvider;
import com.github.loki4j.logback.json.LogLevelJsonProvider;
import com.github.loki4j.logback.json.LoggerNameJsonProvider;
import com.github.loki4j.logback.json.MdcJsonProvider;
import com.github.loki4j.logback.json.MessageJsonProvider;
import com.github.loki4j.logback.json.StackTraceJsonProvider;
import com.github.loki4j.logback.json.ThreadNameJsonProvider;
import com.github.loki4j.logback.json.TimestampJsonProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.ContextAwareBase;

public class JsonLayout extends ContextAwareBase implements Layout<ILoggingEvent> {

    private static final String EMPTY_STRING = "";

    private TimestampJsonProvider timestamp;
    private LoggerNameJsonProvider loggerName;
    private LogLevelJsonProvider logLevel;
    private ThreadNameJsonProvider threadName;
    private MessageJsonProvider message;
    private StackTraceJsonProvider stackTrace;
    private MdcJsonProvider mdc;

    private volatile boolean started;

    private JsonEventWriter jsonWriter;

    private List<JsonProvider<ILoggingEvent>> providers;

    private List<JsonProvider<ILoggingEvent>> customProviders = List.of();

    @Override
    public String doLayout(ILoggingEvent event) {
        var standard = providers.iterator();
        var custom = customProviders.iterator();
        var firstFieldWritten = false;
        jsonWriter.writeBeginObject();
        while (standard.hasNext() || custom.hasNext()) {
            var provider = standard.hasNext() ? standard.next() : custom.next();
            if (!provider.isEnabled() || !provider.canWrite(event))
                continue;

            if (firstFieldWritten)
                jsonWriter.writeFieldSeparator();
            provider.writeTo(jsonWriter, event);

            firstFieldWritten = true;
        }
        jsonWriter.writeEndObject();
        return jsonWriter.toString();
    }

    @Override
    public void start() {
        jsonWriter = new JsonEventWriter(100);  // TODO: fix hardcoding

        timestamp = ensureProvider(timestamp, TimestampJsonProvider::new);
        loggerName = ensureProvider(loggerName, LoggerNameJsonProvider::new);
        logLevel = ensureProvider(logLevel, LogLevelJsonProvider::new);
        threadName = ensureProvider(threadName, ThreadNameJsonProvider::new);
        message = ensureProvider(message, MessageJsonProvider::new);
        stackTrace = ensureProvider(stackTrace, StackTraceJsonProvider::new);
        mdc = ensureProvider(mdc, MdcJsonProvider::new);

        providers = List.of(
                timestamp,
                loggerName,
                logLevel,
                threadName,
                message,
                stackTrace,
                mdc
        );

        for (var provider : customProviders) {
            provider.setContext(context);
            provider.start();
        }

        started = true;
    }
    
    @Override
    public void stop() {
        started = false;

        for (var provider : providers) {
            provider.stop();
        }
        for (var provider : customProviders) {
            provider.stop();
        }
    }

    private <T extends JsonProvider<ILoggingEvent>> T ensureProvider(T current, Supplier<T> factory) {
        if (current != null)
            return current;

        var newInstance = factory.get();
        newInstance.setContext(context);
        newInstance.start();
        return newInstance;
    }

    public void setTimestamp(TimestampJsonProvider timestamp) {
        this.timestamp = timestamp;
    }

    public void setLoggerName(LoggerNameJsonProvider loggerName) {
        this.loggerName = loggerName;
    }

    public void setLogLevel(LogLevelJsonProvider logLevel) {
        this.logLevel = logLevel;
    }

    public void setThreadName(ThreadNameJsonProvider threadName) {
        this.threadName = threadName;
    }

    public void setMessage(MessageJsonProvider message) {
        this.message = message;
    }

    public void setStackTrace(StackTraceJsonProvider stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setMdc(MdcJsonProvider mdc) {
        this.mdc = mdc;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public String getFileHeader() {
        return EMPTY_STRING;
    }

    @Override
    public String getPresentationHeader() {
        return EMPTY_STRING;
    }

    @Override
    public String getPresentationFooter() {
        return EMPTY_STRING;
    }

    @Override
    public String getFileFooter() {
        return EMPTY_STRING;
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
    
}
