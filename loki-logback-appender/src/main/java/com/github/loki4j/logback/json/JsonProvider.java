package com.github.loki4j.logback.json;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.spi.LifeCycle;

public interface JsonProvider<Event extends DeferredProcessingAware> extends ContextAware, LifeCycle {

    boolean isEnabled();

    boolean canWrite(Event event);
    
    void writeTo(JsonEventWriter writer, Event event);

    void prepareForDeferredProcessing(Event event);

}
