package com.github.loki4j.logback.json;

import java.io.IOException;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LifeCycle;

public interface JsonProvider<Event extends DeferredProcessingAware> extends ContextAware, LifeCycle {
    
    void writeTo(JsonEventWriter writer, Event event) throws IOException;

    void prepareForDeferredProcessing(Event event);

}
