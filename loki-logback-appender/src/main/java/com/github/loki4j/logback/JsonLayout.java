package com.github.loki4j.logback;

import java.util.List;

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

    private static String EMPTY_STRING = "";

    private volatile boolean started;

    private JsonEventWriter jsonWriter;

    private final List<JsonProvider<ILoggingEvent>> providers = List.of(
            new TimestampJsonProvider(),
            new LoggerNameJsonProvider(),
            new LogLevelJsonProvider(),
            new ThreadNameJsonProvider(),
            new MessageJsonProvider(),
            new StackTraceJsonProvider(),
            new MdcJsonProvider()
    );

    private List<JsonProvider<ILoggingEvent>> customProviders = List.of();

    @Override
    public String doLayout(ILoggingEvent event) {
        var standard = providers.iterator();
        var custom = customProviders.iterator();
        var firstFieldWritten = false;
        jsonWriter.writeBeginObject();
        while (standard.hasNext() || custom.hasNext()) {
            var provider = standard.hasNext() ? standard.next() : custom.next();
            if (!provider.canWrite(event))
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

        for (var provider : providers) {
            provider.setContext(context);
            provider.start();
        }
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
