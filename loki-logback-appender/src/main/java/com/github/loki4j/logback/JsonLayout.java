package com.github.loki4j.logback;

import java.util.List;

import com.github.loki4j.logback.json.JsonEventWriter;
import com.github.loki4j.logback.json.JsonProvider;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.ContextAwareBase;

public class JsonLayout extends ContextAwareBase implements Layout<ILoggingEvent> {

    private static String EMPTY_STRING = "";

    private volatile boolean started;

    private JsonEventWriter jsonWriter;

    private List<JsonProvider<ILoggingEvent>> providers;

    private List<JsonProvider<ILoggingEvent>> customProviders;

    @Override
    public String doLayout(ILoggingEvent event) {
        jsonWriter.writeBeginObject();
        for (var provider : providers) {
            provider.writeTo(jsonWriter, event);
        }
        for (var provider : customProviders) {
            provider.writeTo(jsonWriter, event);
        }
        jsonWriter.writeEndObject();
        return jsonWriter.toString();
    }

    @Override
    public void start() {
        started = true;
    }
    
    @Override
    public void stop() {
        started = false;
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
